timelog = function($) {

  period = 8

  active_in_last_period = false;
  num_of_scrolls = 0;
  num_of_copies = 0;

  // This is required to get around XSS restrictions in browsers
  // when HTML base tag changes the relative URLs
  var base = window.location.host

  $(document).ready(function() {
      $('body').mousemove(function () {
        active_in_last_period = true;
      });
      $(window).scroll(function() {
        active_in_last_period = true;
        num_of_scrolls += 1;
      });
      
      // adds function call to CTRL+C press
    	var isCtrlPressed = false;	
    	$(document).keyup(function(event) {
    		if (event.which == 17) {  // ctrl
    			isCtrlPressed = false;
    		}
    	}).keydown(function(event) {
    		if (event.which == 17) {
    			isCtrlPressed = true;
    		}		
    		if ((event.which == 99) || (event.which == 67) && (isCtrlPressed == true)) {
    			active_in_last_period = true;
          num_of_copies += 1;		
    		}
    	});
      
  });

  upload_activity = function() {
    if (active_in_last_period) {
      $.post('http://' + base + '/activity/update?nologging', { 'checksum': _ap_checksum, 'period': period, 
        'scrolls': num_of_scrolls, 'copies': num_of_copies, 'nologging': 'true', 'uid' : __peweproxy_uid });
      active_in_last_period = false;
      num_of_scrolls = 0;
      num_of_copies = 0;
    }
  }

  setInterval(upload_activity, period * 1000)

}(adaptiveProxyJQuery);
