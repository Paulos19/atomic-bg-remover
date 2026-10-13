# Atomic Background Remover (AI Engine + Kotlin Android)

Aplicação desenvolvida para remoção de fundos complexos em nível atômico com preservação de roupas brancas, capuz, jaquetas e detalhes finos.

## 🚀 Arquitetura do Projeto

1. **Backend Python (FastAPI + BiRefNet / IS-Net / Rembg + OpenCV)**
   - Localização: `/backend`
   - Algoritmo especializado com pós-processamento morfológico para não comer partes brancas de roupas, capuzes ou sapatos.
   - Container Docker pronto para deploy no **Easypanel**.

2. **Frontend Android (Kotlin + Jetpack Compose + Coil + OkHttp)**
   - Localização: `/android`
   - Design moderno dark mode, preview em tempo real e toggles de precisão atômica.

3. **CI/CD no GitHub Actions**
   - Workflow `.github/workflows/android-build.yml` para compilar o APK automaticamente em todo push e disponibilizar para download nos Artifacts.

## 🛠️ Como rodar o backend localmente

```bash
cd backend
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
