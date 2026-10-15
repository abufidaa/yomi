// يومي — بيفتح بسرعة من النسخة المحفوظة، ويجيب التحديث الجديد في الخلفية
const CACHE='yomi-v2';
const CORE=['./','./index.html','./quran.js','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',e=>{self.skipWaiting();e.waitUntil(caches.open(CACHE).then(c=>c.addAll(CORE)))});
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{
  const u=new URL(e.request.url);
  if(e.request.method!=='GET'||u.origin!==location.origin)return;
  if(u.pathname.endsWith('quran.js')){e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request).then(n=>{const c=n.clone();caches.open(CACHE).then(x=>x.put(e.request,c));return n})));return}
  e.respondWith(caches.open(CACHE).then(c=>c.match(e.request).then(cached=>{
    const net=fetch(e.request).then(n=>{if(n.ok)c.put(e.request,n.clone());return n}).catch(()=>cached||c.match('./index.html'));
    return cached||net;
  })));
});
