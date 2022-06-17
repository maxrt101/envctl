# Environment Controller

Allows for temperature monitoring and remote fan control.  

## Components:
 - backend
 - frontend
 - controller

## How to run
Prerequisites: `docker`, `docker-compose`  
Steps:  
 - Clone the repo on remote server
 - Run `docker-compose up`
 - Install tampermonkey chrome extension
 - Create new script and paste `frontend/tampermonkey.js` into it 
 - Make sure the script is enabled
 - Go to wokwi.com
 - Load the project (under `controller/`)
 - Change `DEFAULT_SERVER_IP` to your ip
 - Run the project
