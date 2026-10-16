// Draws the listing's location with Leaflet + OpenStreetMap tiles (free, no API key needed).
(() => {
    const mapElement = document.getElementById("map");
    if (!mapElement || typeof L === "undefined") return;

    // Stored as GeoJSON [longitude, latitude]; Leaflet wants [latitude, longitude].
    const [lng, lat] = JSON.parse(mapElement.dataset.coordinates);

    const map = L.map(mapElement, { scrollWheelZoom: false }).setView([lat, lng], 11);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 18,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map);

    // A soft circle instead of a pin - the exact address is shared after booking.
    L.circle([lat, lng], { radius: 1500, color: "#ff385c", fillColor: "#ff385c", fillOpacity: 0.2 })
        .addTo(map)
        .bindPopup("Exact location provided after booking");
})();
