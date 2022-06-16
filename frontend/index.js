/* Environment Control v1.0 by maxrt */

let clientId = 'webclient-' + makeid(5)
let controllers = []
let envData = []
let controllerIndex = 0
let currentTemp = 25.0
let currentHumidity = 50
let socket
let chart

function makeid(length) {
  var result = ''
  var characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  for (var i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * characters.length))
 }
 return result
}

function clearAlert() {
  document.getElementById('alert').classList.add('d-none')
  document.getElementById('alert').innerHTML = ''
}

function createAlert(text) {
  document.getElementById('alert').classList.remove('d-none')
  document.getElementById('alert').innerHTML = '<div class="alert alert-danger d-flex align-items-center" role="alert"><svg class="bi flex-shrink-0 me-2" width="24" height="24" role="img" aria-label="Danger:"><use xlink:href="#exclamation-triangle-fill"/></svg><div>' + text + '</div></div>'
}

function updateStatus(i) {
  controllerIndex = i
  document.getElementById('controller-name').innerHTML = controllers[i].name
  document.getElementById('controller-status').innerHTML = controllers[i].state == 'online' ? '🟢 ONLINE' : '🔴 OFFLINE'
  updateCurrentData()
  scheduleUpdate()
}

function generateButtons() {
  console.log('generateButtons: ' + controllers)
  document.getElementById('controller-buttons').innerHTML = ""
  for (var i = 0; i < controllers.length; i++) {
    let html = '<button type="button" id="' + controllers[i].name +'" class="mb-2 btn btn-outline-primary" onclick="updateStatus(' + i + ')">' + controllers[i].name + '</div>'
    document.getElementById('controller-buttons').innerHTML += html
  }
}

function getControllers() {
  let data = {
    command: 'get_controllers',
    sender: clientId,
    data: {}
  }
  controllers = true
  socket.send(JSON.stringify(data));
}

function getData(controller) {
  let data = {
    command: 'get_data',
    sender: clientId,
    data: {
      controller: controller
    }
  }
  envData = true
  socket.send(JSON.stringify(data));
}

function handleController(data) {
  controllers = data.data.controllers
  generateButtons()
}

function handleEnvData(data) {
  envData = data.data.env_data
}

function initWebsocketConnection() {
  socket = new WebSocket('ws://localhost:8080/api');

  socket.addEventListener('message', ev => {
    let data = JSON.parse(ev.data);
    if ('status' in data) {
      console.log('[WS] Got response: ' + ev.data);
      if (data.status == 'error') {
        createAlert('Request failed. Response: ' + data.message)
        setTimeout(clearAlert, 3000)
      }
      if (controllers == true) {
        handleController(data)
      }
      if (envData == true) {
        handleEnvData(data)
      }
    } else if ('command' in data) {
      console.log('[WS] Got command: ' + ev.data);
    }
  });

  socket.addEventListener('open', ev => {
    console.log('[WS] Connection Opened')
    let data = {
      command: 'register',
      sender: clientId,
      data: {
        type: 'client'
      }
    }
    socket.send(JSON.stringify(data));
  });

  socket.addEventListener('close', ev => {
    console.log('[WS] Connection Closed')
  });
}

function fanStart() {
  let data = {
    command: 'fan_start',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex],
      speed: parseInt(document.getElementById('speedInput').innerHTML)
    }
  }
  socket.send(JSON.stringify(data))
}

function fanStop() {
  let data = {
    command: 'fan_stop',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex]
    }
  }
  socket.send(JSON.stringify(data))
}

function temperatureRule() {
  let data = {
    command: 'fan_rule',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex],
      speed: parseInt(document.getElementById('speedInput').innerHTML),
      threshold: parseInt(document.getElementById('temperatureInput').innerHTML)
    }
  }
  socket.send(JSON.stringify(data))
}

// [{x: string, y: float}]
function renderChart(data) {
  document.getElementById("graph").innerHTML = ""
  chart = new ApexCharts(document.getElementById("graph"), {
    chart: {
      animations: {
        enabled: false,
      },
      height: 600,
      toolbar: {
        show: false
      },
      zoom: {
        enabled: false
      }
    },
    series: [{
        data: data
    }],
    yaxis: {
      type: 'category'
    },
    xaxis: {
      opposite: true,
    }
  })
  chart.render();
}

function scheduleUpdate() {
  setTimeout(() => {
    console.log('scheduleUpdate: ' + envData)
    console.log(envData)
    // if (typeof envData != 'object') {
      // scheduleUpdate()
    // } else {
      renderChart(envData.map(v => ({x: v.timepoint, y: v.temperature})))
    // }
  }, 2000);
}

function updateCurrentData() {
  if (socket.readyState == 1) {
    getData(controllers[controllerIndex].name)
    scheduleUpdate()
  }
}

function entryPoint() {
  getControllers();
  setTimeout(updateCurrentData, 2000);
  setInterval(updateCurrentData, 20000);
}

initWebsocketConnection();
setTimeout(entryPoint, 1000);
