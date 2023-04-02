console.log("this is script file");

const togglesidebar = () => {

	if ($(".sidebar").is(":visible")) {
		//true
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "1%");
	} else {
		//false
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "18%");
	}
};