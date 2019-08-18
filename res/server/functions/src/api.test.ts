import * as api from './api';

it("should pass", () => {

  api.extractYoutubeVideosIds('katy perry', 2)
    .then(value => {
      expect(value).toBeTruthy();
      expect(value.length).toBe(2);

      api.extractYoutubeVideos(value).then(value2 => {
        expect(value2).toBeTruthy();
        expect(value2.length).toBe(2);
      }, err2 => {
        expect(err2).toBeFalsy();
      })
    })
    .catch(err => {
      expect(err).toBeFalsy();
    })

});
