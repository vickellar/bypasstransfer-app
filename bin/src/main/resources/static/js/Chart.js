

<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<canvas id="volumeChart"></canvas>

<script>
const ctx = document.getElementById('volumeChart');
new Chart(ctx, {
    type: 'line',
    data: {
        labels: [[${dates}]],
        datasets: [{
            label: 'Daily Volume',
            data: [[${amounts}]]
        }]
    }
});
</script>
