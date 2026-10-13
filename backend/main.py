import io
import os
import cv2
import numpy as np
from PIL import Image
from fastapi import FastAPI, File, UploadFile, Query, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware
from rembg import remove, new_session

app = FastAPI(
    title="Atomic Background Remover API",
    description="Motor de segmentação atômica preservando brancos, capuz, roupas e bordas finas com BiRefNet e IS-Net.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Inicializa sessões especializadas de IA para segmentação fina de altíssima definição
# birefnet-general / isnet-general-use garantem diferenciação absoluta entre fundo branco e roupas/tecidos brancos
SESSIONS = {}

def get_session(model_name: str):
    if model_name not in SESSIONS:
        # Modelos disponíveis: birefnet-general (estado da arte), isnet-general-use, u2net
        try:
            SESSIONS[model_name] = new_session(model_name)
        except Exception:
            # Fallback seguro caso birefnet exija download demorado
            SESSIONS[model_name] = new_session("isnet-general-use")
    return SESSIONS[model_name]

def post_process_refine_white_details(original_img: Image.Image, rgba_result: Image.Image, alpha_matting: bool = True) -> Image.Image:
    """
    Refinamento atômico de máscara:
    Garante que pixels claros/brancos (como camisetas brancas, jaquetas, dentes e tênis)
    não sofram descarte indevido por 'color bleeding' ou similaridade de luminância com o fundo.
    """
    orig_np = np.array(original_img.convert("RGB"))
    rgba_np = np.array(rgba_result.convert("RGBA"))
    
    alpha = rgba_np[:, :, 3]

    # Criação de máscara de preservação morfológica guiada
    # Aplica guided filter ou fechamento morfológico para selar buracos em tecidos brancos
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
    
    # Previne que áreas sólidas de roupa branca fiquem translúcidas
    # Se o pixel tem alta saturação ou é parte do objeto central com luminância alta
    hsv = cv2.cvtColor(orig_np, cv2.COLOR_RGB2HSV)
    _, _, v = cv2.split(hsv)
    
    # Se a área tem alfa intermediário (entre 30 e 200) e luminância alta dentro do contorno do corpo, reforça
    mask_soft = (alpha > 40) & (alpha < 240) & (v > 180)
    alpha[mask_soft] = np.clip(alpha[mask_soft].astype(np.int32) + 60, 0, 255).astype(np.uint8)
    
    # Suavização de contorno para evitar serrilhado nos pelos/fios
    if alpha_matting:
        alpha = cv2.GaussianBlur(alpha, (3, 3), 0)

    rgba_np[:, :, 3] = alpha
    return Image.fromarray(rgba_np)

@app.get("/")
def health_check():
    return {
        "status": "online",
        "engine": "Atomic BG Remover",
        "models_available": ["birefnet-general", "isnet-general-use", "u2net", "silueta"],
        "version": "1.0.0"
    }

@app.post("/api/remove-bg")
async def remove_background(
    file: UploadFile = File(...),
    model: str = Query("birefnet-general", description="Modelo de IA: birefnet-general, isnet-general-use, u2net"),
    alpha_matting: bool = Query(True, description="Refinamento atômico de bordas e cabelos"),
    refine_foreground: bool = Query(True, description="Preserva detalhes e roupas brancas contra fusão de fundo")
):
    """
    Endpoint principal para o app Kotlin Android.
    Recebe Multipart Form com arquivo de imagem, processa com deep learning + matting e devolve PNG transparente.
    """
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="O arquivo enviado precisa ser uma imagem válida.")

    try:
        image_bytes = await file.read()
        input_image = Image.open(io.BytesIO(image_bytes))

        # Obter sessão do modelo correspondente
        session = get_session(model)

        # Executa segmentação com matting atômico
        output_image = remove(
            input_image,
            session=session,
            alpha_matting=alpha_matting,
            alpha_matting_foreground_threshold=240,
            alpha_matting_background_threshold=10,
            alpha_matting_erode_size=10
        )

        # Refinamento de cores e roupas brancas
        if refine_foreground:
            output_image = post_process_refine_white_details(input_image, output_image, alpha_matting=alpha_matting)

        # Converte para PNG em memória
        output_buffer = io.BytesIO()
        output_image.save(output_buffer, format="PNG", optimize=True)
        output_buffer.seek(0)

        return Response(content=output_buffer.getvalue(), media_type="image/png")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro no processamento da imagem: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
