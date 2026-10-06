# Security Specification — GudangPintar

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every warehouse item (`/users/{userId}/items/{itemId}`) and stock movement (`/users/{userId}/movements/{movementId}`) belongs exclusively to `userId` and can only be read or written when `request.auth != null && request.auth.uid == userId`.
2. **Schema & Bounds Integrity**:
   - `InventoryItem`: `sku` (1..40 chars), `name` (1..120 chars), `category` (1..60 chars), `locationRack` (1..60 chars), `quantity` (int >= 0), `minStock` (int >= 0), `unit` (1..30 chars), `unitPrice` (int >= 0), `supplierName` (0..100 chars), `notes` (0..500 chars).
   - `StockMovement`: `type` must be in `['IN', 'OUT', 'ADJUST']`, `quantityChanged` (int >= 0), `previousStock` (int >= 0), `newStock` (int >= 0), `referenceDoc` (0..80 chars), `partyName` (0..100 chars), `notes` (0..500 chars).
3. **Immutable Fields**:
   - On `InventoryItem` update: `userId` and `createdAt` cannot be modified.
   - `StockMovement` logs are append-only (`update: if false`), preserving audit trail integrity; only owner deletion is permitted for cleanup.
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be Firestore `timestamp` and `<= request.time`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get`/`list`/`create` on `/users/alice/items`.
2. **Cross-Tenant Read**: `auth.uid = "bob"` reading `/users/alice/items/item_1`.
3. **Identity Spoofing on Create**: `auth.uid = "alice"` creating `/users/alice/items/item_1` with `userId: "bob"`.
4. **Shadow Field Injection on Create**: Adding `"isAdmin": true` to `/users/alice/items/item_1`.
5. **Shadow Field Injection on Update**: Updating `/users/alice/items/item_1` with `"hacked": "yes"`.
6. **Negative Stock Injection**: Creating or updating `InventoryItem` with `quantity: -50`.
7. **Oversized String DoS**: Creating `InventoryItem` with a 5,000-character `name`.
8. **Invalid Movement Type Enum**: Creating `StockMovement` with `type: "STEAL"`.
9. **Timestamp Manipulation**: Creating `InventoryItem` with `createdAt: "2026-01-01"` (string instead of timestamp).
10. **Immutable Field Mutation**: Updating `InventoryItem.createdAt` or `InventoryItem.userId`.
11. **Audit Log Tampering**: Attempting `update` on `/users/alice/movements/mov_1`.
12. **ID Poisoning**: Document ID containing invalid characters or exceeding 128 chars.
