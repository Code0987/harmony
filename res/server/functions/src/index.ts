import * as functions from 'firebase-functions';

// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript

export const hello = functions.https.onRequest((req, res) => {
  res.send("Hello!");
});

import * as apiController from './api';
export const api = functions.https.onRequest(apiController.app);
