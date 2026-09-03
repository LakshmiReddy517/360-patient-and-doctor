import { Fragment, useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { EquipmentCategory, EquipmentItem } from '../types';

const CATEGORIES: EquipmentCategory[] = ['OXYGEN', 'MOBILITY', 'MONITORING', 'RESPIRATORY', 'OTHER'];
const CATEGORY_BADGE: Record<EquipmentCategory, string> = {
  OXYGEN: 'badge-open', MOBILITY: 'badge-progress', MONITORING: 'badge-hold',
  RESPIRATORY: 'badge-emergency', OTHER: 'badge-normal',
};

export default function InventoryPage() {
  const [items, setItems] = useState<EquipmentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Add / update equipment form.
  const [name, setName] = useState('');
  const [category, setCategory] = useState<EquipmentCategory>('OXYGEN');
  const [unit, setUnit] = useState('');
  const [totalQuantity, setTotalQuantity] = useState('0');
  const [saving, setSaving] = useState(false);

  // Inline allocation panel state (per-item).
  const [allocFor, setAllocFor] = useState<number | null>(null);
  const [allocQty, setAllocQty] = useState('1');
  const [allocCaseId, setAllocCaseId] = useState('');
  const [allocating, setAllocating] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    api.get<EquipmentItem[]>('/inventory/equipment')
      .then((r) => setItems(r.data))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function saveEquipment() {
    if (!name.trim()) { setError('Name is required'); return; }
    setSaving(true); setError('');
    try {
      await api.put<EquipmentItem>('/inventory/equipment', {
        name: name.trim(), category, unit: unit || undefined, totalQuantity: Number(totalQuantity), active: true,
      });
      setName(''); setUnit(''); setTotalQuantity('0'); setCategory('OXYGEN');
      load();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  function openAllocate(item: EquipmentItem) {
    if (allocFor === item.id) { setAllocFor(null); return; }
    setAllocFor(item.id); setAllocQty('1'); setAllocCaseId('');
  }

  async function allocate(item: EquipmentItem) {
    setAllocating(true); setError('');
    try {
      await api.post('/inventory/allocate', {
        equipmentItemId: item.id,
        caseId: allocCaseId ? Number(allocCaseId) : undefined,
        quantity: Number(allocQty),
      });
      setAllocFor(null);
      load();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setAllocating(false); }
  }

  const totalUnitsAvailable = items.reduce((s, i) => s + i.availableQuantity, 0);
  const totalUnitsAllocated = items.reduce((s, i) => s + i.allocatedQuantity, 0);

  if (loading) return <div className="spinner">Loading inventory…</div>;

  return (
    <div>
      <div className="page-head"><h2>Equipment Inventory</h2></div>
      {error && <div className="error-text">{error}</div>}

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Item types</div><div className="value primary">{items.length}</div></div>
        <div className="card stat"><div className="label">Units available</div><div className="value">{totalUnitsAvailable}</div></div>
        <div className="card stat"><div className="label">Units allocated</div><div className="value">{totalUnitsAllocated}</div></div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Add / update equipment</h3></div>
        <div className="card-pad" style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ flex: 2, minWidth: 160 }}><label>Name</label><input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="Oxygen cylinder" /></div>
          <div className="field" style={{ flex: 1, minWidth: 140 }}><label>Category</label><select className="input" value={category} onChange={(e) => setCategory(e.target.value as EquipmentCategory)}>{CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}</select></div>
          <div className="field" style={{ flex: 1, minWidth: 100 }}><label>Unit</label><input className="input" value={unit} onChange={(e) => setUnit(e.target.value)} placeholder="pcs" /></div>
          <div className="field" style={{ flex: 1, minWidth: 100 }}><label>Total qty</label><input className="input" type="number" value={totalQuantity} onChange={(e) => setTotalQuantity(e.target.value)} /></div>
          <button className="btn btn-primary" onClick={saveEquipment} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Name</th><th>Category</th><th>Unit</th><th>Total</th><th>Allocated</th><th>Available</th><th>Active</th><th>Actions</th></tr></thead>
            <tbody>
              {items.length === 0 ? <tr><td colSpan={8} className="spinner">No equipment</td></tr> : items.map((i) => (
                <Fragment key={i.id}>
                  <tr>
                    <td style={{ fontWeight: 600 }}>{i.name}</td>
                    <td><span className={'badge ' + CATEGORY_BADGE[i.category]}>{i.category}</span></td>
                    <td className="muted">{i.unit || '—'}</td>
                    <td className="mono">{i.totalQuantity}</td>
                    <td className="mono">{i.allocatedQuantity}</td>
                    <td className="mono" style={{ fontWeight: 700, color: i.availableQuantity > 0 ? 'var(--success, #16a34a)' : 'var(--danger, #dc2626)' }}>{i.availableQuantity}</td>
                    <td>{i.active ? <span className="badge badge-progress">active</span> : <span className="badge badge-closed">inactive</span>}</td>
                    <td><button className="btn btn-sm" onClick={() => openAllocate(i)} disabled={i.availableQuantity === 0}>Allocate</button></td>
                  </tr>
                  {allocFor === i.id && (
                    <tr>
                      <td colSpan={8} style={{ background: 'var(--surface-2, rgba(148,163,184,.08))' }}>
                        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
                          <div className="field" style={{ minWidth: 120 }}><label>Quantity</label><input className="input" type="number" value={allocQty} onChange={(e) => setAllocQty(e.target.value)} /></div>
                          <div className="field" style={{ minWidth: 140 }}><label>Case id (optional)</label><input className="input" value={allocCaseId} onChange={(e) => setAllocCaseId(e.target.value)} placeholder="e.g. 12" /></div>
                          <button className="btn btn-primary btn-sm" onClick={() => allocate(i)} disabled={allocating}>{allocating ? 'Allocating…' : 'Confirm allocate'}</button>
                          <button className="btn btn-sm" onClick={() => setAllocFor(null)}>Cancel</button>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
