/** Papery "System Vitals" monitor — a calm ECG: mostly-flat baseline with a spike
 *  every so often, scrolling slowly. */
export default function HeartbeatMonitor() {
  // one beat occupies 170 units: long flat line, a single QRS spike, a small T bump
  const beat = (x: number, r: number) => {
    const p: [number, number][] = [
      [0, 22], [74, 22], [80, 24], [84, 22 - r], [88, 28], [92, 22],
      [108, 22], [113, 20.5], [118, 22], [170, 22],
    ];
    return p.map(([px, py]) => [px + x, py] as [number, number]);
  };
  const rAmps = [15, 12.5, 16, 13.5, 15, 12.5];
  const order = [0, 1, 2, 3, 4, 5, 0, 1]; // 6 beats + 2 repeats → seamless −1020 scroll
  let pts: [number, number][] = [];
  order.forEach((bi, i) => { pts = pts.concat(beat(i * 170, rAmps[bi])); });
  const d = pts.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ');

  return (
    <div className="hb-card">
      <div className="hb-top">
        <span className="hb-live" aria-hidden="true" />
        <span className="hb-label">System Vitals</span>
        <span className="hb-bpm"><b>72</b><span>bpm</span></span>
      </div>
      <div className="hb-screen">
        <div className="hb-grid" aria-hidden="true" />
        <svg className="hb-wave" viewBox="0 0 200 44" preserveAspectRatio="none" aria-hidden="true">
          <g className="hb-scroll">
            <path d={d} fill="none" stroke="url(#hbg)" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
          </g>
          <defs>
            <linearGradient id="hbg" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0" stopColor="#0f9a86" stopOpacity=".3" />
              <stop offset=".75" stopColor="#0f9a86" />
              <stop offset="1" stopColor="#14b89f" />
            </linearGradient>
          </defs>
        </svg>
        <span className="hb-sweep" aria-hidden="true" />
      </div>
      <div className="hb-foot">All systems operational</div>
    </div>
  );
}
