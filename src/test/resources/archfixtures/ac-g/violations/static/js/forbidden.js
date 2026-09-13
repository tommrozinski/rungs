const amount = parseFloat(field.value);
const count = parseInt(field.value, 10);
const total = Number(field.value);
label.textContent = amount.toFixed(2);
const rounded = Math.round(amount);
fetch("/v1/assessments");
const request = new XMLHttpRequest();
