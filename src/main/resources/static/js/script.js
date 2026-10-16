// Small enhancements used on every page. The site still works without JavaScript -
// the server validates everything again (see validators/schemas.js).

// 1. Bootstrap form validation: forms with .needs-validation show friendly messages before submitting.
document.querySelectorAll(".needs-validation").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
        }
        form.classList.add("was-validated");
    });
});

// 2. Confirm before destructive actions:  <form data-confirm="Delete this listing?">
document.querySelectorAll("form[data-confirm]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
});

// 3. Submit a form as soon as a <select data-autosubmit> changes (used for "Sort by").
document.querySelectorAll("select[data-autosubmit]").forEach((select) => {
    select.addEventListener("change", () => select.form.submit());
});

// 4. Check-out must be at least one day after check-in.
document.querySelectorAll("[data-checkin]").forEach((checkIn) => {
    const checkOut = checkIn.form && checkIn.form.querySelector("[data-checkout]");
    if (!checkOut) return;

    const syncMinCheckout = () => {
        if (!checkIn.value) return;
        const nextDay = new Date(checkIn.value);
        nextDay.setDate(nextDay.getDate() + 1);
        const min = nextDay.toISOString().slice(0, 10);
        checkOut.min = min;
        if (checkOut.value && checkOut.value < min) checkOut.value = min;
    };
    checkIn.addEventListener("change", syncMinCheckout);
    syncMinCheckout();
});
