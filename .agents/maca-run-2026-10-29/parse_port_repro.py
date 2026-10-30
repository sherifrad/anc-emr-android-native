# Faithful Python port of parseDeliveryNotes from uncommitted.diff (case-insensitive 'contains')
def has(s,k): return k.lower() in s.lower()
def strip_prefix(s):
    if ":" in s:
        r=s.split(":",1)[1].strip()
        return r if r else s
    return s
def parse(notes):
    lines=[l.strip() for l in (notes or "").splitlines() if l.strip()]
    post=[l for l in lines if has(l,"postpartum") or has(l,"delivered") or has(l,"discharged")]
    fees=[l for l in lines if l not in post and (has(l,"fee") or has(l,"EGP") or has(l,"cost") or has(l,"package"))]
    rest=[l for l in lines if l not in post and l not in fees]
    hl=next((l for l in rest if has(l,"hospital")),None)
    hosp=None
    if hl:
        i=hl.lower().rfind(" at ")
        hosp=(hl[i+4:] if i>=0 else strip_prefix(hl)).strip().rstrip(".")
    j=lambda a:"\n".join(strip_prefix(x) for x in a) or None
    return dict(hospital=hosp,fees=j(fees),plan="\n".join(rest) or None,postpartum=j(post))
cases=["Feels reduced fetal movements","Previous pregnancy: delivered by CS 2019","Reviewed 10:30, fee 15,000 EGP",
"Fee quoted (NOT agreed): 15,000 EGP","Elective CS at Cleopatra Hospital at 39 weeks","Plan CS at Cleopatra Hospital. Fee 15,000 EGP",
"Breastfeeding counselling done","PPH prophylaxis: oxytocin after delivery of baby"]
for c in cases: print(repr(c),"->",parse(c))
import re
def rh(g):
    g=g.strip(); return g.endswith("-") or "neg" in g.lower()
for g in ["O -ve","B Rh -ve","O −","A+, antibody screen negative","AB Negative","A-","O pos"]: print(g,rh(g))
