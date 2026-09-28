const API = { products: "/api/products", reorder: "/api/products/reorder", bills: "/api/bills" };
const $ = (selector, root = document) => root.querySelector(selector);
const money = value => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(value || 0));
const state = { products: [], bills: [], lowOnly: false, search: "", editBillId: null };
const productModal = $("#product-modal"), billModal = $("#bill-modal");

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]);
}
async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { "Content-Type": "application/json", ...(options.headers || {}) } });
  const body = await response.text();
  let data;
  try { data = body ? JSON.parse(body) : null; } catch { data = body; }
  if (!response.ok) throw new Error(typeof data === "string" ? data : data?.message || data?.error || `Request failed (${response.status})`);
  return data;
}
function toast(message) {
  const node = $("#toast"); node.textContent = message; node.classList.add("show");
  clearTimeout(toast.timer); toast.timer = setTimeout(() => node.classList.remove("show"), 3000);
}
function isLow(product) { return Number(product.stockQuantity) <= Number(product.reorderThreshold); }
function renderProducts() {
  const products = state.products.filter(product => (!state.lowOnly || isLow(product)) && product.name.toLowerCase().includes(state.search));
  $("#stat-products").textContent = state.products.length;
  $("#stat-low").textContent = state.products.filter(isLow).length;
  $("#product-count").textContent = state.products.length;
  $("#table-summary").textContent = `Showing ${products.length} product${products.length === 1 ? "" : "s"}`;
  const body = $("#product-rows");
  if (!products.length) {
    body.innerHTML = `<tr><td colspan="5" class="empty-state"><strong>${state.search || state.lowOnly ? "No matching products" : "Your catalog is ready for its first product"}</strong>${state.search || state.lowOnly ? "Try changing your search or filter." : "Add a product to start tracking your inventory."}</td></tr>`;
    return;
  }
  body.innerHTML = products.map(product => `<tr><td class="product-name">${escapeHtml(product.name)}</td><td class="price">${money(product.price)}</td><td><span class="stock-number">${Number(product.stockQuantity)}</span> <span style="color:#a2a8a3">units</span></td><td><span class="status ${isLow(product) ? "status-low" : "status-ok"}">${isLow(product) ? "Restock soon" : "In stock"}</span></td><td><div class="bill-actions"><button type="button" data-edit="${Number(product.id)}">Edit</button><button class="delete-bill" type="button" data-delete-product="${Number(product.id)}">Delete</button></div></td></tr>`).join("");
}
async function loadProducts() {
  $("#product-rows").innerHTML = '<tr><td colspan="5" class="loading">Loading your inventory…</td></tr>';
  try { state.products = await request(API.products); renderProducts(); }
  catch (error) { $("#product-rows").innerHTML = `<tr><td colspan="5" class="empty-state"><strong>Could not load inventory</strong>${escapeHtml(error.message)}. Check that the app and database are running.</td></tr>`; toast("Inventory could not be loaded"); }
}
async function loadSales() {
  const date = new Date(); const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  try { $("#stat-sales").textContent = money(await request(`${API.bills}/sales?date=${localDate}`)); }
  catch { $("#stat-sales").textContent = "—"; }
}
async function loadBills() {
  try {
    state.bills = await request(API.bills);
    $("#bill-count").textContent = state.bills.length;
    $("#bill-summary").textContent = `${state.bills.length} bill${state.bills.length === 1 ? "" : "s"} · newest first`;
    const rows = $("#bill-rows");
    if (!state.bills.length) { rows.innerHTML = '<tr><td colspan="6" class="empty-state"><strong>No bills yet</strong>Create a bill to record your first sale.</td></tr>'; return; }
    rows.innerHTML = [...state.bills].sort((a, b) => b.id - a.id).map(bill => {
      const date = bill.billDate ? new Date(bill.billDate).toLocaleString() : "—";
      const items = (bill.items || []).map(item => `${escapeHtml(item.product?.name || "Product")} × ${Number(item.quantity)}`).join(", ");
      const actions = bill.finalized ? '<span style="color:#a1a7a2">Completed</span>' : `<div class="bill-actions"><button type="button" data-edit-bill="${Number(bill.id)}">Edit</button><button type="button" data-finalize-bill="${Number(bill.id)}">Finalize</button><button type="button" class="delete-bill" data-delete-bill="${Number(bill.id)}">Delete</button></div>`;
      return `<tr><td class="product-name">#${Number(bill.id)}</td><td>${escapeHtml(date)}</td><td>${items || "—"}</td><td class="price">${money(bill.totalAmount)}</td><td><span class="bill-status ${bill.finalized ? "finalized" : "draft"}">${bill.finalized ? "Finalized" : "Draft"}</span></td><td>${actions}</td></tr>`;
    }).join("");
  } catch (error) { $("#bill-rows").innerHTML = `<tr><td colspan="6" class="empty-state"><strong>Could not load bills</strong>${escapeHtml(error.message)}</td></tr>`; }
}
function openProduct() { const form = $("#product-form"); delete form.dataset.editId; form.reset(); $(".modal-head h2", form).textContent = "Add a product"; $("#product-error").textContent = ""; productModal.showModal(); }
function addBillLine() {
  const line = document.createElement("div"); line.className = "bill-line";
  const options = state.products.map(product => `<option value="${Number(product.id)}">${escapeHtml(product.name)} — ${money(product.price)} (${Number(product.stockQuantity)} in stock)</option>`).join("");
  line.innerHTML = `<label>Product<select required ${state.products.length ? "" : "disabled"}><option value="">Choose a product</option>${options}</select></label><label>Qty<input type="number" min="1" step="1" required value="1" ${state.products.length ? "" : "disabled"}></label><button class="remove-line" type="button" aria-label="Remove item">×</button>`;
  const select = line.querySelector("select");
  const qty = line.querySelector("input");
  const validateLine = () => {
    const productId = Number(select.value);
    const quantity = Number(qty.value || 0);
    const product = state.products.find(item => Number(item.id) === productId);
    if (!product) return;
    if (quantity > Number(product.stockQuantity)) {
      qty.setCustomValidity(`Only ${product.stockQuantity} units available for ${product.name}.`);
      qty.reportValidity();
      return;
    }
    qty.setCustomValidity("");
  };
  select.addEventListener("change", validateLine);
  qty.addEventListener("input", validateLine);
  line.querySelector(".remove-line").addEventListener("click", () => { if ($$(".bill-line").length > 1) line.remove(); else line.querySelector("select").value = ""; });
  $("#bill-lines").append(line);
}
function $$(selector, root = document) { return [...root.querySelectorAll(selector)]; }
function getBillItemsFromForm() {
  return $$(".bill-line", $("#bill-form")).map(line => ({
    productId: Number($("select", line).value),
    quantity: Number($("input", line).value)
  }));
}
function validateBillItems() {
  const items = getBillItemsFromForm();
  for (const item of items) {
    if (!item.productId || !item.quantity) {
      return "Choose a product and quantity for every line.";
    }
    const product = state.products.find(entry => Number(entry.id) === Number(item.productId));
    if (!product) {
      return "One of the selected products could not be found.";
    }
    if (Number(item.quantity) > Number(product.stockQuantity)) {
      return `Only ${product.stockQuantity} units of ${product.name} are available. Please reduce the quantity.`;
    }
  }
  return "";
}
async function openBill() {
  state.editBillId = null; await loadProducts(); $("#bill-lines").replaceChildren(); addBillLine(); $("#bill-error").textContent = ""; $("#bill-form .modal-head h2").textContent = "Create a bill"; billModal.showModal();
}

$("#today-label").textContent = new Intl.DateTimeFormat("en", { weekday: "long", month: "long", day: "numeric" }).format(new Date()).toUpperCase();
$("#add-product-top").addEventListener("click", openProduct);
$("#add-product-action").addEventListener("click", openProduct);
$("#create-bill-action").addEventListener("click", openBill);
$("#new-bill-button").addEventListener("click", openBill);
$("#refresh-products").addEventListener("click", () => { loadProducts(); loadSales(); });
$("#search").addEventListener("input", event => { state.search = event.target.value.trim().toLowerCase(); renderProducts(); });
$("#low-stock-filter").addEventListener("click", event => { state.lowOnly = !state.lowOnly; event.currentTarget.classList.toggle("selected", state.lowOnly); renderProducts(); });
$("#view-low-stock").addEventListener("click", () => { state.lowOnly = true; $("#low-stock-filter").classList.add("selected"); renderProducts(); });
$("#product-rows").addEventListener("click", async event => {
  const deleteButton = event.target.closest("[data-delete-product]");
  if (deleteButton) {
    const product = state.products.find(item => Number(item.id) === Number(deleteButton.dataset.deleteProduct));
    if (!product || !confirm(`Delete ${product.name}? Products referenced by bills cannot be deleted.`)) return;
    try { await request(`${API.products}/${product.id}`, { method: "DELETE" }); await loadProducts(); toast("Product deleted"); }
    catch (error) { toast(error.message); }
    return;
  }
  const button = event.target.closest("[data-edit]"); if (!button) return;
  const product = state.products.find(item => Number(item.id) === Number(button.dataset.edit)); if (!product) return;
  const form = $("#product-form"); form.elements.name.value = product.name; form.elements.price.value = product.price;
  form.elements.stockQuantity.value = product.stockQuantity; form.elements.reorderThreshold.value = product.reorderThreshold;
  $("#product-error").textContent = ""; productModal.showModal();
  form.dataset.editId = product.id; $(".modal-head h2", form).textContent = "Update product";
});
$("#product-form").addEventListener("submit", async event => {
  event.preventDefault(); const form = event.currentTarget, data = Object.fromEntries(new FormData(form));
  data.price = Number(data.price); data.stockQuantity = Number(data.stockQuantity); data.reorderThreshold = Number(data.reorderThreshold);
  const id = form.dataset.editId; $("#product-error").textContent = "";
  try {
    await request(id ? `${API.products}/${id}` : API.products, { method: id ? "PUT" : "POST", body: JSON.stringify(data) });
    delete form.dataset.editId; $(".modal-head h2", form).textContent = "Add a product"; productModal.close();
    await loadProducts(); toast(id ? "Product updated" : "Product added");
  } catch (error) { $("#product-error").textContent = error.message; }
});
$("#add-bill-line").addEventListener("click", addBillLine);
$("#bill-form").addEventListener("submit", async event => {
  event.preventDefault(); $("#bill-error").textContent = "";
  const items = getBillItemsFromForm();
  const validationMessage = validateBillItems();
  if (validationMessage) { $("#bill-error").textContent = validationMessage; return; }
  try {
    const bill = await request(state.editBillId ? `${API.bills}/${state.editBillId}` : API.bills, { method: state.editBillId ? "PUT" : "POST", body: JSON.stringify({ items }) });
    if (state.editBillId) { state.editBillId = null; billModal.close(); await loadBills(); toast(`Bill #${bill.id} updated`); return; }
    billModal.close(); const finalize = confirm(`Bill #${bill.id} created for ${money(bill.totalAmount)}. Finalize it now? Finalizing deducts stock.`);
    if (finalize) { await request(`${API.bills}/${bill.id}/finalize`, { method: "POST" }); toast(`Bill #${bill.id} finalized`); await loadProducts(); await loadSales(); }
    else toast(`Bill #${bill.id} created`);
    await loadBills();
  } catch (error) { $("#bill-error").textContent = error.message; }
});
$("#bill-rows").addEventListener("click", async event => {
  const edit = event.target.closest("[data-edit-bill]"), finalize = event.target.closest("[data-finalize-bill]"), remove = event.target.closest("[data-delete-bill]");
  try {
    if (edit) {
      const bill = state.bills.find(item => Number(item.id) === Number(edit.dataset.editBill)); if (!bill) return;
      await loadProducts(); state.editBillId = bill.id; $("#bill-lines").replaceChildren();
      for (const item of bill.items || []) { addBillLine(); const line = $$(".bill-line").at(-1); $("select", line).value = item.product.id; $("input", line).value = item.quantity; }
      $("#bill-form .modal-head h2").textContent = `Edit bill #${bill.id}`; $("#bill-error").textContent = ""; billModal.showModal();
    } else if (finalize) {
      if (!confirm("Finalize this bill and deduct stock?")) return;
      await request(`${API.bills}/${finalize.dataset.finalizeBill}/finalize`, { method: "POST" }); await Promise.all([loadBills(), loadProducts(), loadSales()]); toast("Bill finalized and stock updated");
    } else if (remove) {
      if (!confirm("Delete this draft bill?")) return;
      await request(`${API.bills}/${remove.dataset.deleteBill}`, { method: "DELETE" }); await loadBills(); toast("Draft bill deleted");
    }
  } catch (error) { toast(error.message); }
});
$$('[data-close]').forEach(button => button.addEventListener("click", () => { const form = $("#product-form"); delete form.dataset.editId; $(".modal-head h2", form).textContent = "Add a product"; state.editBillId = null; $("#bill-form .modal-head h2").textContent = "Create a bill"; document.getElementById(button.dataset.close).close(); }));
for (const dialog of [productModal, billModal]) dialog.addEventListener("click", event => { if (event.target === dialog) dialog.close(); });
loadProducts(); loadSales();
loadBills();
