#!/bin/bash
# setup-stable-diffusion.sh

echo "Setting up Stable Diffusion WebUI..."

# Clone WebUI
git clone https://github.com/AUTOMATIC1111/stable-diffusion-webui
cd stable-diffusion-webui

# Install requirements
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
pip install -r requirements.txt

# Download SDXL model
mkdir -p models/Stable-diffusion
cd models/Stable-diffusion
wget https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0/resolve/main/sd_xl_base_1.0.safetensors

# Download ControlNet models
cd ../..
mkdir -p models/ControlNet
cd models/ControlNet
wget https://huggingface.co/lllyasviel/sd_control_collection/resolve/main/diffusers_xl_depth_full.safetensors

echo "Setup complete!"
echo "Run: ./webui.sh --api --listen --port 7860"

# Start Stable Diffusion WebUI
cd stable-diffusion-webui
./webui.sh --api --listen --port 7860

# Test API
curl -X POST http://localhost:7860/sdapi/v1/txt2img \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "professional doctor",
    "steps": 20,
    "width": 512,
    "height": 512
  }'