kw = function($) {

  // This is required to get around XSS restrictions in browsers
  // when HTML base tag changes the relative URLs
  var base = window.location.host;

  var retries = 0;

  function fetchKeywords() {
    $.get('http://' + base + '/keywords/load', {'checksum': _ap_checksum}, function(data) {
      if(!/^\s*$/.test(data)) {
        clearTimeout(timer);
        $.notifyBar({
          html: data,
          delay: 5000,
          animationSpeed: "normal",
          close: true
        });

      }

      retries++;

      if(retries > 3) {
        clearTimeout(timer)
      }
    });
  }

  $(document).ready(function() {
    setTimeout(fetchKeywords, 1000);
    timer = setInterval(fetchKeywords, 5000);
  });

}(adaptiveProxyJQuery);
