import express from "express";
import router from "./api/router.js";

const app = express();

app.use(express.json());

app.use("/talk", router);

const server = app.listen(8080, () => {
  const host = server.address().address;
  const port = server.address().port;
  console.log(`Example app listening at http://${host}:${port}`);
});
