function todayISO() {
  const d = new Date();
  const m = String(d.getMonth()+1).padStart(2,'0');
  const day = String(d.getDate()).padStart(2,'0');
  return `${d.getFullYear()}-${m}-${day}`;
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('date').value = todayISO();
  loadStats();
  loadWorkouts();
  document.getElementById('workout-form').addEventListener('submit', onSubmit);
});

async function onSubmit(e) {
  e.preventDefault();
  const date = document.getElementById('date').value.trim();
  const exercise = document.getElementById('exercise').value.trim();
  const setsRaw = document.getElementById('sets').value.trim();
  const durationVal = document.getElementById('duration').value;
  const duration = durationVal ? parseInt(durationVal, 10) : null;

  const sets = parseSets(setsRaw);
  if (!exercise) return alert('Exercise is required');
  if (!date) return alert('Date is required');
  if (sets.length === 0 && !(duration && duration > 0)) {
    return alert('Add sets or a cardio duration.');
  }

  const body = { date, exercise, sets: sets.length ? sets : null, duration };
  const res = await fetch('/api/workouts', {
    method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body)
  });
  const out = await res.text();
  if (!res.ok) {
    try { const j = JSON.parse(out); alert(j.error || 'Error'); } catch { alert(out); }
    return;
  }
  document.getElementById('sets').value = '';
  document.getElementById('duration').value = '';
  await loadStats();
  await loadWorkouts();
}

function parseSets(text) {
  if (!text) return [];
  const lines = text.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  const sets = [];
  for (const line of lines) {
    // Accept "5,135" or "5 135"
    const parts = line.split(/[,\s]+/).filter(Boolean);
    if (parts.length >= 2) {
      const reps = parseInt(parts[0], 10);
      const weight = parseFloat(parts[1]);
      if (!isNaN(reps) && !isNaN(weight) && reps > 0 && weight >= 0) {
        sets.push({ reps, weight });
      }
    }
  }
  return sets;
}

async function loadStats() {
  const res = await fetch('/api/stats'); const data = await res.json();
  document.getElementById('weekly').textContent = data.weeklyVolume ?? 0;
  document.getElementById('monthly').textContent = data.monthlyVolume ?? 0;
  renderBests(data.bestOneRm || {});
  drawChart(data.dailyVolumes || []);
}

function renderBests(map) {
  const el = document.getElementById('bests');
  const entries = Object.entries(map).sort((a,b)=>a[0].localeCompare(b[0]));
  if (entries.length === 0) { el.textContent = 'No 1RM data yet.'; return; }
  el.innerHTML = '<strong>Best 1RM:</strong> ' + entries.map(([k,v]) => `${k}: ${v}`).join(' • ');
}

async function loadWorkouts() {
  const res = await fetch('/api/workouts'); const list = await res.json();
  const tbody = document.querySelector('#table tbody');
  tbody.innerHTML = '';
  list.slice(-20).reverse().forEach(w => {
    const tr = document.createElement('tr');
    const details = w.sets && w.sets.length
      ? w.sets.map(s => `${s.reps}x${s.weight}`).join(', ')
      : (w.duration ? `${w.duration} min` : '-');
    tr.innerHTML = `<td>${w.date}</td><td>${escapeHtml(w.exercise)}</td><td>${details}</td>`;
    tbody.appendChild(tr);
  });
}

function drawChart(dailies) {
  const svg = document.getElementById('chart');
  const W = svg.clientWidth || 700;
  const H = svg.clientHeight || 220;
  while (svg.firstChild) svg.removeChild(svg.firstChild);

  if (!dailies.length) return;

  const xs = dailies.map((d,i)=>i);
  const ys = dailies.map(d=>d.volume);
  const minY = 0;
  const maxY = Math.max(...ys, 10);
  const pad = 24;

  const points = xs.map((x,i) => {
    const px = pad + (x / Math.max(xs.length-1,1)) * (W - 2*pad);
    const py = H - pad - ((ys[i]-minY) / (maxY-minY)) * (H - 2*pad);
    return `${px},${py}`;
  }).join(' ');

  const axis = document.createElementNS('http://www.w3.org/2000/svg','line');
  axis.setAttribute('x1', String(pad));
  axis.setAttribute('y1', String(H - pad));
  axis.setAttribute('x2', String(W - pad));
  axis.setAttribute('y2', String(H - pad));
  axis.setAttribute('stroke', '#23273a');
  axis.setAttribute('stroke-width', '2');
  svg.appendChild(axis);

  const poly = document.createElementNS('http://www.w3.org/2000/svg','polyline');
  poly.setAttribute('fill','none');
  poly.setAttribute('stroke','#5eead4');
  poly.setAttribute('stroke-width','3');
  poly.setAttribute('points', points);
  svg.appendChild(poly);
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
}
