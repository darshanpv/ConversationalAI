// JavaScript Document
var cardsFlag = false;

$('#error').hide();
//config:
/*------------ Local variable ------------------*/
var debug = true;
//console.log(location + ' ' + '   master location');
var browserSupportsSpeech = false;
var wants_speech = false;

var systemUtteranceContent;
var userUtteranceContent;

/*-------------------------- On Submit Function Click and Enter Start --------------------------*/
$("#btnSubmit").click(function () {
    submit();
});
//enter
$('#userUtterance').keydown(function (e) {
    if (e.keyCode == 13) {
        submit();
    }
});
/*-------------------------- On Submit Function Click and Enter End --------------------------*/

/*-------------------------- On Submit Function Call Start  --------------------------*/

var udateAvatar;
updateAvatar = 'img/avatar1.png';
var userUtteranceVal;
var trimUserUtterance = '';

function submit() {
    $('#loading-small').show();
    userUtteranceVal = $("#userUtterance").val();
    userUtteranceValNew = $("#userUtterance").val();
    trimUserUtterance = $.trim(userUtteranceVal);

    /*----------- Exit app if bye utterance start---------------*/

    if (trimUserUtterance == "bye" || trimUserUtterance == "bye bye" || trimUserUtterance == "exit" || trimUserUtterance == "thank you bye bye") {
        $('#confirm').modal({
            backdrop: 'static',
            keyboard: false,
            focus: this
        });
        $('#confirm').on('shown.bs.modal', function () {
            //alert('before exit btn..');
            $('#exitApp').focus();
        })
        $(document.body).on('click', '#NoExitApp', function () {
            $("#loading-small").hide();
            $("#userUtterance").val('');
        });
        $(document.body).on('click', '#exitApp', function () {
            $.post(locationInt + '/terminate',
                function (data) {
                    console.log(data + '...Exit app...');
                }
            );
            $(".chat-wrapper, #home-wrapper, #welcome-wrapper").fadeOut("fast");
            $("#login-wrapper").fadeIn();
            $("#username, #password, #userUtterance").val('');
            $("#loading-small").hide();
            location.reload();
        });
    }
    /*----------- Exit app if bye utterance END---------------*/

    /*----------------- Submit Post Call Start--------------------*/
    function submitPostCall() {
        // console.log("step 4...");
        //console.log(userUtteranceVal)
        // console.log(locationInt);
        $.post(locationInt, {
                userUtterance: userUtteranceVal
            }, function (data) {
                //console.log("step 5...");                
                getCurrentTask();
                $('#loading-small').hide();
                // systemUtteranceVal = data.message.chat;
                if ($('#selectLanguage').is(':visible') && $("#selectLanguage").val() !== "en" && $("#btnText").hasClass("active")) {
                    var locationLanguageSelection = location1 + "/translateLanguage?sourceLanguage=en&targetLanguage=" + selectLanguageValue + "&sourceText=" + data.result.reply;

                    $.post(locationLanguageSelection, function (data) {
                        //console.log(data);
                        systemUtteranceVal = data.targetText;
                        submitPostCallInner();
                    });
                } else if ($('#select_language').is(':visible') && $("#select_language").val() !== "en-GB") {
                    //console.log(selectLanguageValue);
                    var locationLanguageSelection = location1 + "/translateLanguage?sourceLanguage=en&targetLanguage=" + selectLanguageValue + "&sourceText=" + data.result.reply;
                    //console.log(locationLanguageSelection);
                    $.post(locationLanguageSelection, function (data) {
                        //console.log(data);
                        systemUtteranceVal = data.targetText;
                        submitPostCallInner();
                    });
                } else {
                    //alert('test..')                   
                    systemUtteranceVal = data.result.reply;
					systemSpeechVal = data.result.speech;
                    submitPostCallInner();
                }

                function submitPostCallInner() {
                    systemUtteranceContentResponse = '<li class="mar-btm systemUtterance"><div class="media-body pad-hor"> <div class="speech"> <p id="">' + systemUtteranceVal + '</p><p class="speech-time"><i class="fa fa-clock-o fa-fw"></i>' + currentTime() + '</p> </div></div></li>';
                    userUtteranceContent = '<li class="mar-btm userUtterance"><div class="media-body pad-hor speech-right"><div class="speech"><p id="userUtteranceValue">' + userUtteranceValNew + '</p><p class="speech-time"><i class="fa fa-clock-o fa-fw"></i>' + currentTime() + '</p></div></div></li>';

                    if (!data.message == 0) {
                        infoSystemResponseText = '<div class="info-content-success"><div class="message">' + data.message.data.info.text + '</div></div><a href="#." class="readMore">Read More</a>'
                        errorSystemResponseText = '<div class="info-content-error"><div class="message">' + data.message.data.info.text + '</div></div>'

                        if (!data.message.data.info == 0) {
                            if (!data.message.data.info.text == 0) {
                                $('#info-wrapper .info-content').html(infoSystemResponseText).fadeIn(600);

                                $('.readMore').magnificPopup({
                                    items: {
                                        src: '<div class="white-popup">' + data.message.data.info.text + '</div>',
                                        type: 'inline'
                                    },
                                    closeBtnInside: true
                                });
                            }

                            if (!data.message.data.info.video == 0) {
                                $('#info-wrapper .info-video').html(data.message.data.info.video).fadeIn(600);
                            }
                            if (!data.message.data.info.image == 0) {
                                var firstimg = $(data.message.data.info.image).find("img:first").attr("src");
                                $('#info-wrapper .info-image').html('<a class="image-popup-fit-width cursor-zoom pop" href="' + firstimg + '">' + data.message.data.info.image + '</a>').fadeIn(600);

                                $('.image-popup-fit-width').magnificPopup({
                                    type: 'image',
                                    closeOnContentClick: true,
                                    image: {
                                        verticalFit: false
                                    }
                                });
                            }


                        } else {
                            $('#info-wrapper .info-content').html('<img src="img/text-placeholder2.jpg" class="placeholder" alt="Text placeholder">');
                            $('#info-wrapper .info-video').html('<img src="img/video-placeholder2.jpg" class="placeholder" alt="video placeholder">');
                            $('#info-wrapper .info-image').html('<img src="img/image-placeholder2.jpg" class="placeholder" alt="Image placeholder">');
                        }
                        if (!data.message.data.error == 0) {
                            $('#info-wrapper .info-content').html(errorSystemResponseText).fadeIn(600);
                        }
                    }

                    if (!$.trim($("#userUtterance").val())) {
                        console.log('textaarea is empty');
                    } else {
                        $("#utterance").append(userUtteranceContent).fadeIn(600);
                        $("#utterance").append(systemUtteranceContentResponse).fadeIn(600);
                    }

                    //is user has used ASR before, automatically start TTS
                    if (wants_speech) {
                        tts_msg.lang = select_language.value;
                        /*tts_msg.text = data.message.chat                            
                        speechSynthesis.speak(tts_msg);*/
                        if (select_language.value == "hi") {
                            responsiveVoice.speak("" + systemSpeechVal + "", "Hindi Female");
                            speechSynthesis.speak(tts_msg);
                        } else if (select_language.value == "de-DE") {
                            responsiveVoice.speak("" + systemSpeechVal + "", "Deutsch Female");
                            speechSynthesis.speak(tts_msg);
                        } else if (select_language.value == "it-IT") {
                            responsiveVoice.speak("" + systemSpeechVal + "", "Italian Female");
                            speechSynthesis.speak(tts_msg);
                        } else if (select_language.value == "fr-FR") {
                            responsiveVoice.speak("" + systemSpeechVal + "", " French Female");
                            speechSynthesis.speak(tts_msg);
                        } else if (select_language.value == "pt-PT") {
                            responsiveVoice.speak("" + systemSpeechVal + "", " Portuguese Female");
                            speechSynthesis.speak(tts_msg);
                        } else {
                            responsiveVoice.speak("" + systemSpeechVal + "", "UK English Female");
                            speechSynthesis.speak(tts_msg);
                        }
                    }
                    $("#userUtterance").val('');

                    $("#scroll-wrap").mCustomScrollbar("scrollTo", "bottom", {
                        scrollEasing: "easeOut"
                    });

                    /*-------------------- Card Code Start Here--------------------*/
                    //if(cardsFlag == true) {
                    // fnCards();                        
                    //}
                    /*-------------------- Card Code Ends Here--------------------*/
                }

            })
            .fail(
                function (xhRequest, status,
                    thrownError) {
                    console.log(status);
                    console.log(thrownError);
                });
    }
    /*----------------- Submit Post Call End--------------------*/

    /*---------------- Text/voice Select language function if/else start -------------*/

    if ($('#selectLanguage').is(':visible') && $("#selectLanguage").val() !== "en") {
        var selectLanguageValue = $("#selectLanguage").val();
        changeLanguage(userUtteranceVal, selectLanguageValue);
    } else if ($('#select_language').is(':visible') && $("#select_language").val() !== "en-GB") {
        var selectLanguageValue = $("#select_language").val();
        changeLanguage(userUtteranceVal, selectLanguageValue);
    } else {
        //userUtteranceValNew = $("#userUtterance").val();
        if ($('#select_language').is(':visible') && $("#select_language").val() == "en-GB") {
            //  var selectLanguageValue = "en";
            submitPostCall();
            //changeLanguage(userUtteranceVal, selectLanguageValue);
        }
    }

    function changeLanguage(srcTxt, selectLanguageVal) {
        //console.log("step 2...");
        var locationLanguageSelection = location1 + "/translateLanguage?sourceLanguage=" + selectLanguageValue + "&targetLanguage=en&sourceText=" + srcTxt;
        $.post(locationLanguageSelection, function (data) {
            //console.log("step 3...");
            userUtteranceVal = data.targetText;
            userUtteranceValNew = data.sourceText;
            submitPostCall();
        });
    }

    /*---------------- Text/voice Select language if/else function End -------------*/

    /*------------------------- spell correct if condiotion start ---------------*/
    if ($("#btnText").hasClass("active") && $("#text-auto-mode").is(':checked') && $("#selectLanguage").val() == "en") {
        wants_speech = false;
        locationSpellChecker = location1 + "/spellChecker";
        console.log(locationSpellChecker);
        $.post(locationSpellChecker, {
                userUtterance: userUtteranceVal
            },
            function (data) {
                console.log(data);
                userUtteranceVal = data.response;
                userUtteranceValNew = data.response;
                submitPostCall();
            }
        );
    } else {
        if ($('#selectLanguage').is(':visible') && $("#selectLanguage").val() == "en") {
            submitPostCall();
        }
    }
    /*------------------------- spell correct if condiotion End ---------------*/

}

$("#selectLanguage").change(function () {
    selectLanguageValue = $("#selectLanguage").val();
});
/*-------------------------- On Submit Function Call End  --------------------------*/

/*-------------------------- Show Error Function Start  --------------------------*/
function showError(error, fatal) {
    if (error != null || error != undefined || error != 'not-allowed') {
        $('#error').show(0).delay(3500).hide(0);
        $("#error").html(error);
    } else if (fatal) {
        $('#loader').removeClass("loading");
        $('#container').delay(3500).animate({
            opacity: .2
        }, 2500);

    } else {
        $("#error:hidden:first").fadeIn(1000).delay(2000).fadeOut(1000);
    }
}
/*-------------------------- Show Error Function End  --------------------------*/

/*-------------------------------- Google Web Speech API Start ---------------------*/

if (!('SpeechRecognition' in window) && !('webkitSpeechRecognition' in window)) {
    showError(
        "Your Browser doesn't support speech recognition. Try Chrome.",
        false);
    $('#recobtn').prop('disabled', true);

} else {
    // if($("#btnVoice").hasClass("active")){

    browserSupportsSpeech = true;
    var auto_submit = true;
    //TTS
    var tts_msg = new SpeechSynthesisUtterance();
    //tts_msg.lang = 'en-GB';
    //alert(select_language.value);
    tts_msg.lang = select_language.value;

    tts_msg.onend = function (e) {
        start_stop_recognition();
        //recognition.stop();
    };
    //ASR
    var recognizing = false;
    var final_transcript = '';
    var ignore_onend = false;
    var start_timestamp;
    var SpeechRecognition = SpeechRecognition || webkitSpeechRecognition;
    var recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;

    /*------------ speech recognition OnError Event Start------------------*/
    recognition.onerror = function (event) {
        ignore_onend = true;
        //updateGui(false);
        wants_speech = false;
        if ($("#btnVoice").hasClass("active")) {

            if (event.error == 'no-speech') {
                start_img.src = 'img/mic.png';
                showInfo('info_no_speech');
            }
            if (event.error == 'audio-capture') {
                start_img.src = 'img/mic.png';
                showInfo('info_no_microphone');
            }
            if (event.error == 'not-allowed') {
                if (event.timeStamp - start_timestamp < 100) {
                    showInfo('info_blocked');
                } else {
                    showInfo('info_denied');
                }
            }
        } else {
            start_img.src = 'img/mic-disabled.png';
        }

    };
    /*------------ speech recognition OnError Event End------------------*/

    /*------------ speech recognition OnStart Event Start------------------*/

    recognition.onstart = function () {
        recognizing = true;
        start_img.src = 'img/mic-animate.gif';
        showInfo('info_speak_now');
        //console.log('Speech recognition service has started');
    };
    /*------------ speech recognition OnStart Event End------------------*/

    /*------------ speech recognition OnEnd Event Start------------------*/
    recognition.onend = function () {
        recognizing = false;
        if (ignore_onend) {
            return;
        }
        if (!recognition.continuous) {
            recognition.stop();
        }
        start_img.src = 'img/mic.png';
        if (!final_transcript) {

            showInfo('info_start');
            return;
        }
        showInfo('');
        if (window.getSelection) {
            window.getSelection().removeAllRanges();
            var range = document.createRange();
            range.selectNode(document.getElementById('final_span'));
            window.getSelection().addRange(range);
        }
    };
    /*------------ speech recognition OnEnd Event End------------------*/

    /*------------ speech recognition OnResult Event Start------------------*/
    recognition.onresult = function (event) {
        var interim_transcript = '';
        if (typeof (event.results) == 'undefined') {
            recognition.onend = null;
            recognition.stop();
            showError("Result undefined", false);
            return;
        }

        for (var i = event.resultIndex; i < event.results.length; ++i) {
            if (event.results[i].isFinal) {
                final_transcript += event.results[i][0].transcript;
            } else {
                interim_transcript += event.results[i][0].transcript;
            }
        }
        $("#userUtterance").val(final_transcript);
        if (!recognition.continuous) {
            recognition.stop();
        }
        if (auto_submit)
            submit();
    };
    /*------------ speech recognition OnResult Event End------------------*/
    /*------------ Function  start stop recognition start------------------*/
    window.start_stop_recognition = function () {
            if ($("#btnVoice").hasClass("active")) {
                if (recognizing) {
                    recognition.stop();
                    wants_speech = false;
                    start_img.src = 'img/mic.png';
                    ignore_onend = true;
                    return;
                }
                wants_speech = true;
                final_transcript = '';
                recognition.lang = select_language.value;
                if ($('#voice-auto-mode').is(':checked')) {
                    recognition.start();
                }
                if ($('#voice-manual-mode').is(':checked')) {
                    recognition.stop();
                }
                ignore_onend = false;
                start_img.src = 'img/mic.png';
            } else {
                start_img.src = 'img/mic-disabled.png';
            }
        }
        /*------------ Function  start stop recognition End------------------*/
        /*------------ Show Info Function start------------------*/
    function showInfo(s) {
        if (s) {
            for (var child = info.firstChild; child; child = child.nextSibling) {
                if (child.style) {
                    child.style.display = child.id == s ? 'inline' : 'none';
                }
            }
            $("#info").show().delay(3500).fadeOut(1000);

        } else {
            $("#info").hide();
        }
    }
    /*------------ Show Info Function end------------------*/


    //}
    /*------------ Record button start------------------*/
    $("#recobtn").click(function (event) {

        if ($("#btnVoice").hasClass("active")) {
            start_stop_recognition();
        }
        start_img.src = 'img/mic-slash.png';
        if (recognizing) {
            console.log('inside recognizing...');
            recognition.stop();
            return;
        }
        if (!$('#voice-auto-mode').is(':checked')) {
            recognition.start();
        }
    });
    /*------------ Record button End------------------*/
}
/*-------------------------------- Google Web Speech API End ---------------------*/


/*----------------- Get Date function Start--------------------*/
function getDate() {
    var d = new Date();
    var year = d.getFullYear();
    var date = d.getDate();
    var days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    var months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    var day = days[d.getDay()];
    var month = months[d.getMonth()];
    return day + ', ' + month + ' ' + date + ', ' + year;
}
$('.date').text(getDate());
/*----------------- Get Date function End--------------------*/


/*------------- Set Column Height Start -----------------*/
function setHeight() {
    windowHeight = $(window).innerHeight();
    $('#task-details').css('height', windowHeight - 64);
    $("#scroll-wrap").css('height', windowHeight - 480);
};
setHeight();
$(window).resize(function () {
    setHeight();
});
/*------------- Set Column Height End -----------------*/

/*------------ Exit chat start--------------------*/
$('#exit, .icon-exit').click(function () {
    $.post(locationInt + '/terminate',
        function (data) {
            console.log(data + '...inside exit data...')
        }
    );

    $("#login-wrapper").fadeIn("fast");
    $(".chat-wrapper, #home-wrapper, #welcome-wrapper").fadeOut("fast");
    $('#utterance').empty();
    $("#username, #password, #userUtterance").val('');
    $("#loading-small").hide();
    location.reload();
});
/*-------------- Exit chat end-----------------*/

/*----------------- Clear Chat Start ------------------*/
$('#clearChat').click(function () {
    $('#utterance').empty();
});
/*----------------- Clear chat end -------------------*/
$(document).on("click", ".popover .close", function () {
    $(this).parents(".popover").popover('hide');
});

/*------------ Logout Click start --------------*/
$(document).on("click", "#logout", function () {
    $.post(locationInt + '/terminate',
        function (data) {
            console.log(data + '...inside logout...')
        }
    );
    //$(".chat-wrapper").hide(); 
    $(".chat-wrapper, #home-wrapper, #welcome-wrapper").fadeOut("fast");
    $("#login-wrapper").fadeIn();
    $("#username, #password, #userUtterance").val('');
    $("#loading-small").hide();
    location.reload();
});
/*------------ Logout Click End --------------*/

/*---------------- Voice and Text toggle start----------------*/
$('#radioBtn a').on('click', function () {
    var sel = $(this).data('title');
    var tog = $(this).data('toggle');
    $('#' + tog).prop('value', sel);

    $('a[data-toggle="' + tog + '"]').not('[data-title="' + sel + '"]').removeClass('active').addClass('notActive');
    $('a[data-toggle="' + tog + '"][data-title="' + sel + '"]').removeClass('notActive').addClass('active');
})

$("#radio-text-wrapper, #radio-voice-wrapper").hide();

$("#radio-text-wrapper").show();
$("#select_language").hide();
$(document).on("click", "#btnVoice", function () {
    $("#userUtterance").attr('disabled', true);
    $("#btnSubmit").attr('disabled', true);
    $("#recobtn").attr('disabled', false);
    $('#selectLanguage').val('en').trigger('change');
    $("#radio-voice-wrapper").show();
    $("#radio-text-wrapper").hide();

    $("#selectLanguage").hide();

    $("#select_language").show();

    start_img.src = 'img/mic.png';
});
$(document).on("click", "#btnText", function () {
    disableVoice();
    $("#select_language").hide();
    $("#selectLanguage").show();
});
if ($("#btnText").hasClass('active')) {
    disableVoice();
}

/*------------ Function Disabled Voice start------------------*/
function disableVoice() {
    wants_speech = false;
    $("#userUtterance").attr('disabled', false);
    $("#btnSubmit").attr('disabled', false);
    $("#recobtn").attr('disabled', true);

    $("#radio-voice-wrapper").hide();
    $("#radio-text-wrapper").show();

    start_img.src = 'img/mic-disabled.png';
}
/*------------ Function Disabled Voice End------------------*/

/*--------------Voice/Text Switch Toggle Start -----------------*/
$(".switch-toggle label").on('click', function () {
    $('.switch-toggle label').removeClass('active');
    $(this).addClass('active');
});
/*--------------Voice/Text Switch Toggle End -----------------*/

/*---------------- Voice and Text toggle End----------------*/

/*------------------------ Help/Scubscribe popup start --------------------*/
$("a[class*='popup-gallery-']").magnificPopup({
    //$('.open-help-popup').magnificPopup({
    type: 'inline',
    midClick: true // Allow opening popup on middle mouse click. Always set it to true if you don't provide alternative source in href.
});
/*------------------------ Help/Subscribe popup End --------------------*/

/*----------- Translate dropdown (keyboard) lanngauge selection Start ------------*/
$('#selectLanguage').on('change', function () {
    if (this.value == "en") {
        transliterationControl.disableTransliteration();
    } else {
        languageChangeHandler()
    }
});
/*----------- Translate dropdown (keyboard) lanngauge selection End ------------*/