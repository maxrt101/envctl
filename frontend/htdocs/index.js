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

function updateData() {
  console.log('[EC] Update Data')
  chart.updateSeries([{
    data: envData.map(v => {
      let options = {month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit'}
      let date = new Date(Date.parse(v.timepoint))
      return {x: date.toLocaleDateString('en-GB', options), y: v.temperature}
    })
  }])
}

function updateControllerStatus() {
  document.getElementById('controller-name').innerHTML = controllers[controllerIndex].name
  document.getElementById('controller-status').innerHTML = controllers[controllerIndex].state == 'online' ? '🟢 ONLINE' : '🔴 OFFLINE'
}

function setController(i) {
  controllerIndex = i
  updateControllerStatus()
}

function generateButtons() {
  console.log('generateButtons: ' + controllers)
  document.getElementById('controller-buttons').innerHTML = ""
  for (var i = 0; i < controllers.length; i++) {
    let html = '<button type="button" id="' + controllers[i].name +'" class="mb-2 btn btn-outline-primary" onclick="setController(' + i + ')">' + controllers[i].name + '</div>'
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
  document.getElementById('controller-th').innerHTML = envData[envData.length - 1].temperature + '° ' + envData[envData.length - 1].humidity + '%'
  updateControllerStatus()
  updateData()
}

function initWebsocketConnection() {
  socket = new WebSocket('ws://' + location.host + ':8080/api')

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
  })

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
  })

  socket.addEventListener('close', ev => {
    console.log('[WS] Connection Closed')
  })

  socket.addEventListener('error', ev => {
    console.log('[WS] Connection Error')
    createAlert('Connection Error')
  })
}

function fanStart() {
  let data = {
    command: 'fan_start',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex].name,
      speed: parseInt(document.getElementById('speedInput').value)
    }
  }
  socket.send(JSON.stringify(data))
}

function fanStop() {
  let data = {
    command: 'fan_stop',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex].name
    }
  }
  socket.send(JSON.stringify(data))
}

function temperatureRule() {
  let data = {
    command: 'fan_rule',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex].name,
      speed: parseInt(document.getElementById('speedInput').value),
      threshold: parseInt(document.getElementById('temperatureInput').value)
    }
  }
  socket.send(JSON.stringify(data))
}

function renameController() {
  let data = {
    command: 'update_name',
    sender: clientId,
    data: {
      controller: controllers[controllerIndex].name,
      name: document.getElementById('nameInput').value
    }
  }
  socket.send(JSON.stringify(data))
}

// [{x: string, y: float}]
function createChart() {
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
      data: []
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

function updateCurrentData() {
  if (socket.readyState == 1) {
    getControllers()
    setTimeout(() => {
      getData(controllers[controllerIndex].name)
    }, 2500)
  }
}

function entryPoint() {
  createChart()
  getControllers()
  updateCurrentData()
  setInterval(updateCurrentData, 10000)
}

initWebsocketConnection()
setTimeout(entryPoint, 1000)
