using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using System.IO;
using Aurio.Project;
using Aurio.Matching.Chromaprint;
using System.Diagnostics;
using app.references;
using System.Net.Http;
using Newtonsoft.Json;
using RunProcessAsTask;
using MetaBrainz.MusicBrainz;

namespace app.Controllers {
    [Route("api/v{version:apiVersion}/[controller]")]
    [ApiVersion("1.0")]
    public class MusicDBController : Controller {

        [HttpPost("chromaprint")]
        public async Task<JsonResult> Chromaprint([FromBody] byte[] audioData) {
            var error = "";
            var duration = "-1";
            var fingerprint = string.Empty;

            string path = null;
            try {
                // Create temp file
                path = Path.GetTempFileName();

                // Save all data
                await System.IO.File.WriteAllBytesAsync(path, audioData);

                // Generate fingerprint
                var pr = await ProcessEx.RunAsync(
                    "resources/chromaprint-fpcalc-1.4.2-windows-i686/fpcalc.exe",
                    "-json " + path);

                var output = JsonConvert.DeserializeAnonymousType(pr.StandardOutput[0], new { duration, fingerprint });

                duration = ((int)double.Parse(output.duration)).ToString();
                fingerprint = output.fingerprint;

            } catch (Exception e) {
                Debug.WriteLine(e);

                error = e.Message;
            } finally {
                if (!string.IsNullOrEmpty(path))
                    try {
                        System.IO.File.Delete(path);
                    } catch (Exception e) {
                        Debug.WriteLine(e);
                    }
            }

            return new JsonResult(new {
                error,
                data = new { duration, fingerprint }
            });
        }

        [HttpGet("chromaprint_lookup")]
        public async Task<JsonResult> ChromaprintLookup(string duration, [FromBody] string fingerprint) {
            var error = "";
            var data = new Object();

            using (var client = new HttpClient()) {
                try {
                    client.BaseAddress = new Uri("https://api.acoustid.org/");

                    var key = "iwj8azntPz";

                    var response = await client.GetAsync($"v2/lookup?format=json&client={key}&duration={duration}&fingerprint={fingerprint}&meta=recordings+releasegroups+compress");
                    response.EnsureSuccessStatusCode();

                    var responseReultString = await response.Content.ReadAsStringAsync();

                    var responseReult = JsonConvert.DeserializeAnonymousType(responseReultString, new {
                        status = "",
                        results = new [] {
                            new {
                                score = "",
                                id = "",
                                recordings = new [] {
                                    new {
                                        duration = "",
                                        releasegroups = new [] {
                                            new {
                                                type = "",
                                                id = "",
                                                title = ""
                                            }
                                        },
                                        title = "",
                                        id = "",
                                        artists = new [] {
                                            new {
                                                id = "",
                                                name = ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

                    data = responseReult;

                } catch (Exception e) {
                    Debug.WriteLine(e);

                    error = e.Message;
                }
            }

            return new JsonResult(new {
                error,
                data
            });
        }

        [HttpGet("similar")]
        public async Task<JsonResult> SimilarArtist(string q, string type) {
            var error = "";
            var data = new Object();

            try {
                var query = new Query($"harmony-api/{HttpContext.GetRequestedApiVersion().ToString()} ( http://harmony.ilusons.com )");

                var findResult = await query.FindArtistsAsync(q, 3, 0);

            } catch (Exception e) { Debug.WriteLine(e); }

            return new JsonResult(new {
                error,
                data
            });
        }

    }

}
