using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ApiExplorer;
using Microsoft.Extensions.DependencyInjection;

namespace app.Controllers {

    [Route("api")]
    public class ApiController : Controller {

        [HttpGet]
        public string Get() {
            return "Hello !";
        }

        [HttpGet("getApiVersions")]
        public IEnumerable<string> GetApiVersions() {
            var provider = HttpContext.RequestServices.GetRequiredService<IApiVersionDescriptionProvider>();

            foreach (var description in provider.ApiVersionDescriptions) {
                yield return description.ApiVersion.ToString();
            }
        }

    }

}
