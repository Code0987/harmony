import * as api from "./api";

const server = api.app.listen(api.app.get("port") || 8080, () => {
  console.log(
    "  App is running at http://localhost:%d in %s mode",
    api.app.get("port") || 8080,
    api.app.get("env")
  );
  console.log("  Press CTRL-C to stop\n");
});

export default server;
