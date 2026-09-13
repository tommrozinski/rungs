// 027 REQ-011: shows or hides the mortgage section as soon as "I rent" changes, without a server round trip.
// The server renders the correct visibility on every response, so the form still works if this script fails.
document.addEventListener('DOMContentLoaded', function () {
    var rentsCheckbox = document.getElementById('rents');
    var mortgageSection = document.getElementById('mortgage-section');
    if (!rentsCheckbox || !mortgageSection) {
        return;
    }
    rentsCheckbox.addEventListener('change', function () {
        mortgageSection.classList.toggle('is-hidden', rentsCheckbox.checked);
    });
});
