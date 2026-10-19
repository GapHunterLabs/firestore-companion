"""Load the demo data into a local Firestore emulator (never a real project).

    firebase emulators:start --only firestore --project demo-northwind-shop   (from this folder)
    python seed.py                       # wipes the emulator's data, then loads the shop below

Then, in the IDE: Firestore tool window -> Emulator host `127.0.0.1:8080`, Project ID `demo-northwind-shop`, Connect.
Project IDs that start with `demo-` are offline-only for the Firebase tools: no Google account, nothing leaves the machine.
"""
import json
import sys
import urllib.request

HOST = "127.0.0.1:8080"
PROJECT = "demo-northwind-shop"
ROOT = "http://%s/v1/projects/%s/databases/(default)/documents" % (HOST, PROJECT)


def value(v):
    if isinstance(v, bool):
        return {"booleanValue": v}
    if isinstance(v, int):
        return {"integerValue": str(v)}
    if isinstance(v, float):
        return {"doubleValue": v}
    if isinstance(v, dict):
        return {"mapValue": {"fields": {k: value(x) for k, x in v.items()}}}
    if isinstance(v, list):
        return {"arrayValue": {"values": [value(x) for x in v]}}
    if isinstance(v, str) and len(v) == 20 and v[4] == "-" and v.endswith("Z"):
        return {"timestampValue": v}
    return {"stringValue": v}


def request(method, url, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method,
                                 headers={"Authorization": "Bearer owner", "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return resp.read()


def put(path, fields):
    collection, doc_id = path.rsplit("/", 1)
    request("POST", "%s/%s?documentId=%s" % (ROOT, collection, doc_id), {"fields": {k: value(v) for k, v in fields.items()}})


CUSTOMERS = {
    "c_ana_ruiz": {"name": "Ana Ruiz", "email": "ana.ruiz@example.com", "city": "Medellín", "tier": "gold", "orders": 7},
    "c_li_wei": {"name": "Li Wei", "email": "li.wei@example.com", "city": "Toronto", "tier": "silver", "orders": 3},
    "c_marta_silva": {"name": "Marta Silva", "email": "marta.silva@example.com", "city": "Lisbon", "tier": "standard", "orders": 1},
    "c_omar_haddad": {"name": "Omar Haddad", "email": "omar.haddad@example.com", "city": "Amman", "tier": "gold", "orders": 12},
}

PRODUCTS = {
    "p_espresso_beans": {"name": "Espresso beans 1 kg", "price": 24.5, "stock": 140, "active": True, "tags": ["coffee", "beans"]},
    "p_pour_over_kit": {"name": "Pour-over kit", "price": 39.0, "stock": 22, "active": True, "tags": ["brewing"]},
    "p_burr_grinder": {"name": "Burr grinder", "price": 129.0, "stock": 0, "active": False, "tags": ["equipment"]},
    "p_ceramic_mug": {"name": "Ceramic mug", "price": 14.0, "stock": 310, "active": True, "tags": ["merch"]},
}

ORDERS = {
    "ord_1042": ({"customer": "c_ana_ruiz", "status": "pending", "total": 63.5, "createdAt": "2026-10-18T14:05:00Z",
                  "shipping": {"city": "Medellín", "method": "express"}},
                 {"i1": {"product": "p_espresso_beans", "qty": 1, "price": 24.5},
                  "i2": {"product": "p_pour_over_kit", "qty": 1, "price": 39.0}}),
    "ord_1043": ({"customer": "c_omar_haddad", "status": "shipped", "total": 129.0, "createdAt": "2026-10-18T16:40:00Z",
                  "shipping": {"city": "Amman", "method": "standard"}},
                 {"i1": {"product": "p_burr_grinder", "qty": 1, "price": 129.0}}),
    "ord_1044": ({"customer": "c_li_wei", "status": "pending", "total": 42.0, "createdAt": "2026-10-19T09:12:00Z",
                  "shipping": {"city": "Toronto", "method": "standard"}},
                 {"i1": {"product": "p_ceramic_mug", "qty": 3, "price": 14.0}}),
    "ord_1045": ({"customer": "c_marta_silva", "status": "delivered", "total": 24.5, "createdAt": "2026-10-19T11:30:00Z",
                  "shipping": {"city": "Lisbon", "method": "pickup"}},
                 {"i1": {"product": "p_espresso_beans", "qty": 1, "price": 24.5}}),
}


def main():
    try:
        request("DELETE", "http://%s/emulator/v1/projects/%s/databases/(default)/documents" % (HOST, PROJECT))
    except OSError as e:
        sys.exit("The Firestore emulator isn't reachable at %s (%s). Start it first, see the top of this file." % (HOST, e))
    for doc_id, fields in CUSTOMERS.items():
        put("customers/" + doc_id, fields)
    for doc_id, fields in PRODUCTS.items():
        put("products/" + doc_id, fields)
    for doc_id, (fields, items) in ORDERS.items():
        put("orders/" + doc_id, fields)
        for item_id, item in items.items():
            put("orders/%s/items/%s" % (doc_id, item_id), item)
    print("Loaded %d customers, %d products and %d orders into %s on %s."
          % (len(CUSTOMERS), len(PRODUCTS), len(ORDERS), PROJECT, HOST))


if __name__ == "__main__":
    main()
