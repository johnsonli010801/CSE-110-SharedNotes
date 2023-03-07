package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }

    public Note getNote(String title){
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("GET", null)
                .build();
        try (var response = client.newCall(request).execute()) {
            if(response.body() == null) {
                Log.e("GET", "FAIL");
                return null;
            } else {
                var body = response.body().string();
                Log.i("GET", body);
                return Note.fromJSON(body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //json is the string representation of a note.
    public void putNote(Note note){
        var title = note.title.replace(" ", "%20");
        final MediaType JSON = MediaType.get("application/json");

        String json = note.toJSON();
        RequestBody body = RequestBody.create(json, JSON);
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("PUT", body)
                .build();

        var executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try (var response = client.newCall(request).execute()) {
                if (request.body() == null) {
                    Log.e("PUT", "FAIL");
                } else {
                    var bodyString = response.body().string();
                    Log.i("PUT", bodyString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
