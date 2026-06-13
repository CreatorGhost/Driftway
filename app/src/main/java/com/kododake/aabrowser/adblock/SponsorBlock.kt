package com.kododake.aabrowser.adblock

/**
 * SponsorBlock integration for YouTube — skips creator-read sponsor / intro / outro segments
 * via the crowd-sourced public API (https://sponsor.ajay.app). This is NOT ad blocking: it skips
 * segments the creator inserted, not Google's served ads. Stable, low-maintenance (public API),
 * and entirely page-side JS. Injected at document-start for youtube.com origins; it polls for SPA
 * navigation (YouTube changes the video without a full page load).
 */
object SponsorBlock {
    val JS = """
        (function(){
          if (window.__aaSbInit) return; window.__aaSbInit = true;
          var cats = ["sponsor","intro","outro","selfpromo","interaction","music_offtopic"];
          var segments = [];
          var currentId = null;
          var boundVideo = null;
          function getId(){
            try{
              var u = new URL(location.href);
              var v = u.searchParams.get('v');
              if (v) return v;
              if (location.pathname.indexOf('/shorts/') === 0) return location.pathname.split('/')[2] || null;
              return null;
            }catch(e){ return null; }
          }
          function load(id){
            segments = [];
            var url = 'https://sponsor.ajay.app/api/skipSegments?videoID=' +
              encodeURIComponent(id) + '&categories=' + encodeURIComponent(JSON.stringify(cats));
            fetch(url).then(function(r){ return r.ok ? r.json() : []; }).then(function(d){
              if (Array.isArray(d)) {
                segments = d.map(function(s){ return s.segment; }).filter(function(s){
                  return s && s.length === 2 && isFinite(s[0]) && isFinite(s[1]) && s[1] > s[0];
                });
              }
            }).catch(function(){});
          }
          function onTime(){
            var v = boundVideo; if (!v || !segments.length) return;
            var t = v.currentTime;
            for (var i=0;i<segments.length;i++){
              var s = segments[i];
              if (t >= s[0] && t < s[1] - 0.3){ try{ v.currentTime = s[1]; }catch(e){} break; }
            }
          }
          function ensureBound(){
            var v = document.querySelector('video');
            if (v && v !== boundVideo){ boundVideo = v; v.addEventListener('timeupdate', onTime); }
          }
          setInterval(function(){
            var id = getId();
            if (id && id !== currentId){ currentId = id; load(id); }
            ensureBound();
          }, 1000);
        })();
    """.trimIndent()
}
