import * as request from 'request-promise';
import * as ytdl from 'youtube-dl';

export async function extractYoutubeVideosIds(query: string, count: number) {
  const res = await request(
    `https://www.youtube.com/results?search_query=${query}`
  );

  const matches = new Set();

  const re = /\/watch\?v=([0-9A-Za-z_-]{10}[048AEIMQUYcgkosw]*)/g;
  let m = re.exec(res);
  while (m && (matches.size < count)) {
    matches.add(m[1]);
    m = re.exec(res);
  }

  return [...matches];
}

export async function extractYoutubeVideos(ids: unknown[]) {
  const urls = ids.map(x => `http://www.youtube.com/watch?v=${x}`);

  const infos = [];

  for (const url of urls) {
    const info = await new Promise(function (resolve, reject) {
      ytdl.getInfo(url, function (err: any, data: any) {
        if (err) {
          reject(err);
        }
        resolve(data);
      });
    });

    if (info)
      infos.push(info);
  }

  return infos;
}

//// Express ////

import * as express from 'express';
import * as cors from 'cors';

export const app = express();

app.use(cors({ origin: true }));
app.use((req: any, res: any, next: any) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST');
  res.setHeader('Access-Control-Allow-Headers', 'X-Requested-With,content-type');
  res.setHeader('Access-Control-Allow-Credentials', true);

  next();
});

app.get('/ytids', async (req, res) => {
  try {
    const result = await extractYoutubeVideosIds(req.query.keywords, req.query.count || 3);

    res.status(200).send(result);
  } catch (err) {
    res.status(500).send();
  }
});