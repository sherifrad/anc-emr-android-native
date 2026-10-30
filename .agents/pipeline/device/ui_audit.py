import re,sys,xml.etree.ElementTree as ET
dens=float(sys.argv[2])/160; t=ET.parse(sys.argv[1])
for n in t.iter('node'):
    if n.get('clickable')!='true': continue
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    w,h=(x2-x1)/dens,(y2-y1)/dens
    label=n.get('content-desc') or n.get('text') or ''
    if not label:
        label='/'.join(c.get('content-desc') or c.get('text') or '' for c in n.iter('node') if (c.get('content-desc') or c.get('text')))[:50]
    flag=[]
    if min(w,h)<48: flag.append(f'SMALL {w:.0f}x{h:.0f}dp')
    if not label: flag.append('NO LABEL')
    print(('FAIL ' if flag else 'ok   ')+f'{w:4.0f}x{h:<4.0f} {n.get("resource-id","")[-30:]:30} {label[:45]!r} {" ".join(flag)}')
