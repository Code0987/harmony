import * as api from './api';

api.extractYoutubeVideosIds('katy perry', 2)
  .then(value => {
    console.log(value);

    api.extractYoutubeVideos(value).then(value2 => {
      console.log(value2);
    }, err2 => {
      console.log(err2);
    })
  })
  .catch(err => {
    console.log(err);
  })