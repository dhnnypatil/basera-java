// Live price breakdown on the listing page's booking card.
// This is only a preview - the real price is always calculated on the server (services/bookingService.js).
(() => {
    const form = document.getElementById("booking-form");
    if (!form) return;

    const pricePerNight = Number(form.dataset.price);
    const feeRate = Number(form.dataset.feeRate);
    const checkIn = form.querySelector("[data-checkin]");
    const checkOut = form.querySelector("[data-checkout]");
    const breakdown = document.getElementById("price-breakdown");
    const MS_PER_DAY = 24 * 60 * 60 * 1000;

    const rupees = (amount) => "₹" + Math.round(amount).toLocaleString("en-IN");

    function update() {
        const nights = Math.round((new Date(checkOut.value) - new Date(checkIn.value)) / MS_PER_DAY);
        if (!checkIn.value || !checkOut.value || !(nights > 0)) {
            breakdown.classList.add("d-none");
            return;
        }
        const subtotal = nights * pricePerNight;
        const fee = Math.round(subtotal * feeRate);

        document.getElementById("pb-nights").textContent = `${rupees(pricePerNight)} x ${nights} ${nights === 1 ? "night" : "nights"}`;
        document.getElementById("pb-subtotal").textContent = rupees(subtotal);
        document.getElementById("pb-fee").textContent = rupees(fee);
        document.getElementById("pb-total").textContent = rupees(subtotal + fee);
        breakdown.classList.remove("d-none");
    }

    checkIn.addEventListener("change", update);
    checkOut.addEventListener("change", update);
})();
