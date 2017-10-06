using app.Controllers;
using Newtonsoft.Json;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using Xunit;

namespace app.test {

    public class MusicDBControllerUnitTest {

        [Fact]
        public async void ChromaprintTest() {
            var controller = new MusicDBController();

            using (Stream source = File.OpenRead(@"resources\audio_samples\Poets of the Fall - Carnival of Rust.mp3")) {
                byte[] buffer = new byte[source.Length];
                int bytesRead;
                bytesRead = source.Read(buffer, 0, buffer.Length);

                var result = await controller.Chromaprint(buffer);

                var data = JsonConvert.DeserializeAnonymousType(JsonConvert.SerializeObject(result.Value),  new {
                    error = "",
                    data = new { duration = "", fingerprint = "" }
                });

                var result2 = await controller.ChromaprintLookup(data.data.duration, data.data.fingerprint);

                Debug.WriteLine(result2.Value.ToString());

            }

        }

    }

}
