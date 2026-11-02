package com.fehunter.captureflow.features.youtubeplayer.data

/** Builds the local document that hosts YouTube's official IFrame Player API. */
internal object YoutubePlayerHtml {
    fun create(videoId: String, origin: String): String {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Invalid YouTube video ID" }
        require(origin.startsWith("https://") && origin.none { it == '\'' || it == '"' || it == '<' || it == '>' }) {
            "Invalid player origin"
        }
        return """
            <!doctype html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    html, body { width:100%; height:100%; margin:0; padding:0; overflow:hidden; background:#000; }
                    #player-container, #player, #player iframe {
                        position:absolute; inset:0; width:100%; height:100%; border:0; background:#000;
                    }
                </style>
            </head>
            <body>
                <div id="player-container"><div id="player"></div></div>
                <script>
                    var captureFlowPlayer;
                    function onYouTubeIframeAPIReady() {
                        captureFlowPlayer = new YT.Player('player', {
                            width:'100%', height:'100%', videoId:'$videoId',
                            playerVars: { autoplay:0, controls:1, playsinline:1, rel:0, fs:1, origin:'$origin' },
                            events: {
                                onReady:function() { CaptureFlowYoutube.onReady(); },
                                onStateChange:function(event) { CaptureFlowYoutube.onStateChange(event.data); },
                                onError:function(event) { CaptureFlowYoutube.onError(event.data); }
                            }
                        });
                    }
                    (function() {
                        var tag = document.createElement('script');
                        tag.src = 'https://www.youtube.com/iframe_api';
                        tag.async = true;
                        var firstScript = document.getElementsByTagName('script')[0];
                        firstScript.parentNode.insertBefore(tag, firstScript);
                    })();
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
