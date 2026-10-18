// يومي — بيجيب أحدث نسخة من الموقع دايمًا لو فيه نت، ولو مفيش بيفتح آخر نسخة محفوظة
const CACHE='yomi-20261018092511';
const CORE=['./','./index.html','./quran.js','./hisn.js','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',e=>{self.skipWaiting();e.waitUntil(caches.open(CACHE).then(c=>c.addAll(CORE)))});
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{
  const u=new URL(e.request.url);
  if(e.request.method!=='GET'||u.origin!==location.origin)return;
  if(u.pathname.endsWith('version.json'))return;
  if(u.pathname.endsWith('quran.js')){e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request).then(n=>{const c=n.clone();caches.open(CACHE).then(x=>x.put(e.request,c));return n})));return}
  e.respondWith(fetch(e.request,{cache:'no-store'}).then(n=>{if(n.ok){const c=n.clone();caches.open(CACHE).then(x=>x.put(e.request,c))}return n})
    .catch(()=>caches.match(e.request).then(r=>r||caches.match('./index.html'))));
});

self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.matchAll({type:'window',includeUncontrolled:true}).then(cs=>{for(const c of cs){if('focus' in c)return c.focus()}return clients.openWindow('./')}))});
