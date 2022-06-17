// ==UserScript==
// @name         wokwi dht22 data generator
// @namespace    https://wokwi.com/*
// @version      1.0
// @description  supply randomly generated data to dht22
// @author       maxrt
// @match        https://wokwi.com/projects/*
// @icon         data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==
// @grant        none
// ==/UserScript==

const initTimeout = 10000     // timeout for initial start
const timeout = 7500          // timeout for temp update (ms)
const cycleLength = 10        // length of generator
const tMin = 10               // min temperature
const tMax = 20               // max temperature
const hMin = 40               // min humidity
const hMax = 70               // max humidity
const maxFlucDeviation = 0.1  // max deviation of generated value from previous
const maxTempDeviation = 1.5
const maxHumDeviation = 7

let tBase = 20  // base temperature
let hBase = 55  // base humidity
let fluc = []   // fluctuctions array
let i = 0       // cycle iteration

function getRandomRange(min, max) {
    return Math.random() * (max - min) + min;
}

function capToPrev(value, prev, deviation) {
    if (Math.abs(value - prev) > deviation) {
        if (value > prev) {
            return prev + getRandomRange(0, deviation)
        } else {
            return prev - getRandomRange(0, deviation)
        }
    }
    return value
}

function generateTempFluctuations(size) {
    let prev = 1.5
    if (fluc.length > 0) {
        prev = fluc[fluc.length-1]
    }
    fluc = []
    for (var i = 0; i < size; i++) {
        let f = capToPrev(getRandomRange(1, 2), prev, maxFlucDeviation)
        fluc.push(f)
        prev = f
    }
}

function getTemperature(index) {
    return tBase * fluc[index % fluc.length]
}

function getHumidity(index) {
    return hBase * (((fluc[index % fluc.length] - 1) / 2 ) + 1)
}

function updateData(temp, hum) {
    document.getElementsByTagName('wokwi-dht22')[0].setAttribute('temperature', temp)
    document.getElementsByTagName('wokwi-dht22')[0].setAttribute('humidity', hum)
}

function generateBasesAndFluc() {
    tBase = capToPrev(getRandomRange(tMin, tMax), tBase, maxTempDeviation)
    hBase = capToPrev(getRandomRange(hMin, hMax), hBase, maxHumDeviation)
    generateTempFluctuations(cycleLength);
}

window.resetDHT22DataGen = function() { // for use outside of tampermonkey
    i = 0
    generateBasesAndFluc()
}

function main() {
    generateBasesAndFluc()

    console.log('[EXT][TM] Timeout: ' + timeout)

    setInterval(function() {
        if (i % cycleLength == 0) {
            generateBasesAndFluc()
        }
        let t = getTemperature(i)
        let h = getHumidity(i)
        console.log('[EXT][TM] Next values t=' + t + ' h=' + h)
        updateData(t, h)
        i++
    }, timeout)
}

setTimeout(main, initTimeout)
