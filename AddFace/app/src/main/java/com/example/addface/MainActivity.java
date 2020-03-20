package com.example.addface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.example.addface.env.Device;
import com.example.addface.env.DownloadUtil;
import com.example.addface.face.FaceAttr;
import com.example.addface.face.FaceRecognizer;
import com.example.addface.face.MTCNN;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.gotev.speech.Speech;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static com.example.addface.env.ImageUtils.resizeImage;

public class MainActivity extends AppCompatActivity {
    public static final int FACE_SIZE = 112;
    public static final String VERSION_NO = "1.0.0";
    public static final String OPEN_SOURCE_URL ="https://github.com/qiangz520";
    public static final String CONTACT_EMAIL = "zengqiang18@zju.edu.cn";

    //for Http Request
//    public static final String IP = "http://10.214.193.33:80";
    public static final String IP = "http://114.55.218.3:80";
    public static final String GET_FACES_URL = IP + "/pull";
    public static final String UPDATE_URL = IP + "/update";
    public static final String DELETE_URL = IP + "/delete?";
//    public static final String DOWNLOAD_URL = IP + "/download/face_data";

    //for method of getting a photo
    private static final int TAKE_PHOTO = 1;
    private static final int CHOOSE_PHOTO = 2;

//    public  String DestFileDir;
//    public  String DestFileName = "face_data";

    private Uri imageUri;
    private String personName = "";
    private final Device device = Device.GPU;
    private final int numThreads = 4;
    private MTCNN mtcnn;
    private FaceRecognizer faceRecognizer;
    private ImageView faceImageView;
    private TextView result_textView;
    private TextView info_textView;
    private Speech speech;
    private FloatingActionButton add_face_FAB;
    private FloatingActionButton face_list_FAB;
    private static Map<String, String> NameFacesDBMap = new HashMap<>();
    private CharSequence[] cs_names;


    /**
     * 获取并处理人脸数据的Handler
     */
    @SuppressLint("HandlerLeak")
    private  Handler mHandler_getData = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            String faces_data_str = msg.obj.toString();
            JSONObject jsonObject = JSONObject.parseObject(faces_data_str);
            Set<Map.Entry<String, Object>> entrySet = jsonObject.entrySet();
            for(Map.Entry<String, Object> entry: entrySet){
                NameFacesDBMap.put(entry.getKey(), (String) entry.getValue());
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mtcnn = new MTCNN(this);
        faceRecognizer = new FaceRecognizer(this, device, numThreads);
//        DestFileDir = this.getExternalFilesDir(null) + "/AbsTest";
        faceImageView = findViewById(R.id.imageView);
        result_textView = findViewById(R.id.result_text_view);
        info_textView =findViewById(R.id.software_information);


        //初始化语音播报对象，三倍速，队列模式
        speech = Speech.init(this, getPackageName());
        speech.setTextToSpeechRate((float) 3.0);
        speech.setTextToSpeechQueueMode(QUEUE_ADD);

        add_face_FAB = findViewById(R.id.add_face_FAB);
        face_list_FAB = findViewById(R.id.face_list_FAB);


        result_textView.setText(getString(R.string.hello));

        getFacesFromServer();
//        downloadFile(DOWNLOAD_URL);

        add_face_FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                speech.say(getString(R.string.add_good_face));
                Toast.makeText(MainActivity.this, getString(R.string.add_good_face),
                        Toast.LENGTH_SHORT);
                InputName();
            }
        });

        face_list_FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cs_names = getNameList();
//                speech.say(getString(R.string.get_faces_list));
                DisplayFacesList();
            }
        });

        info_textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfo();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭speech对象
        if (speech != null) {
            speech.shutdown();
        }
    }

    /**
     * 显示软件信息
     */
    private void showInfo(){
        new AlertDialog.Builder(this)
                .setTitle("软件信息")
                .setMessage("版本号： "+VERSION_NO
                        +"\n服务器IP： "+IP
                        +"\n软件开源： " +OPEN_SOURCE_URL
                        +"\n软件版权： 浙江大学"
                        +"\n联系我们： "+CONTACT_EMAIL)
                .create()
                .show();
    }

    /**
     * 人脸列表弹窗
     */
    private void DisplayFacesList(){
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_name))
                .setItems(cs_names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int i) {
                         new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.dialog_delete_face_title))
                                .setMessage(getString(R.string.dialog_delete_face_message))
                                .setPositiveButton(getString(R.string.dialog_btn_confirm_text)
                                        , new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                deletePersonInServer(cs_names[i]);
                                                result_textView.setText("你刚刚删除了"+cs_names[i]+"的人脸数据!");
                                                faceImageView.setImageBitmap(null);
                                                dialog.dismiss();
                                            }
                                        })
                                .setNegativeButton(getString(R.string.dialog_btn_cancel_text)
                                        , new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                .create()
                                .show();
                    }
            })
            .show();

    }

    /**
     * @return 从人脸数据中获取姓名列表
     */
    private CharSequence[] getNameList(){
        CharSequence[] names_cs = new CharSequence[NameFacesDBMap.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : NameFacesDBMap.entrySet()) {
            names_cs[i++] = entry.getKey();
        }
        return names_cs;
    }

    /**
     * @param personName 检查输入姓名是否已存在
     * @return
     */
    private boolean isExist(String personName){
        cs_names = getNameList();
        for(int i = 0; i < cs_names.length; i++){
            if(personName.equals(cs_names[i])){
                return true;
            }
        }
        return false;
    }

    /**
     * @param name 删除姓名为Name的人脸数据
     */
    private void deletePersonInServer(final CharSequence name){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .get()
                .url(DELETE_URL + "&name=" + name)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                speech.say(getString(R.string.fail_delete_face));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                speech.say(getString(R.string.succeed_delete_face));
                NameFacesDBMap.remove(name);
            }
        });


    }

    /**
     * 从服务器获取人脸数据
     */
    private void getFacesFromServer(){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()//创建Request 对象。
                .get()
                .url(GET_FACES_URL)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                speech.say(getString(R.string.fail_get_faces));
                Log.e("OKHTTP", "onFailure: ",e);
                Log.d("", "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("OKHTTP", "onResponse: ");
                speech.say(getString(R.string.succeed_get_faces));
                String responseData = response.body().string();
                Message message = new Message();
                message.obj = responseData;
                mHandler_getData.sendMessage(message);
            }
        });
    }

    /**
     * 输入姓名以及选择人脸图片
     */
    private void InputName(){
        final EditText et_input_name = new EditText(this);
        et_input_name.setText("");
        new AlertDialog.Builder(this).setTitle("请输入待添加人脸对应姓名")
                .setIcon(R.drawable.ic_smile)
                .setView(et_input_name)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        personName = et_input_name.getText().toString();
                        if (personName.equals("")) {
                            Toast.makeText(MainActivity.this, getString(R.string.name_is_empty), Toast.LENGTH_SHORT).show();
//                            speech.say(getString(R.string.name_is_empty));
                        }
                        else if(isExist(personName)){
                            Toast.makeText(MainActivity.this, getString(R.string.person_name_existed), Toast.LENGTH_SHORT).show();
//                            speech.say(getString(R.string.person_name_existed));
                        }
                        else{
                            playDialogList();                        }
                        }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     *  显示对话选择列表
     */
    private void playDialogList() {
        final String items[] = {"拍一张照片", "从相册选择"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加一张人脸图片");
        builder.setIcon(R.drawable.ic_smile);
        // 设置列表显示，注意设置了列表显示就不要设置builder.setMessage()了，否则列表不起作用。
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        takePhoto();
                        break;
                    case 1:
                        chooseFromAlbum();
                        break;
                }
            }
        });
        builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /**
     * 拍一张照片
     */

    private void takePhoto(){
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");

        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.example.addface.fileprovider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }

        //启动相机程序
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }
    // 从相册选择
    private void chooseFromAlbum(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    //打开相册
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "you denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        processFaceBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {  //4.4及以上的系统使用这个方法处理图片；
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeKitKat(data);  //4.4及以下的系统使用这个方法处理图片
                    }
                }
            default:
                break;
        }
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }


    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {

            Bitmap faceBitmap = BitmapFactory.decodeFile(imagePath);
            processFaceBitmap(faceBitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 4.4及以上的系统使用这个方法处理图片
     *
     * @param data
     */
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果document类型的Uri,则通过document来处理
            String docID = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docID.split(":")[1];     //解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;

                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/piblic_downloads"), Long.valueOf(docID));

                imagePath = getImagePath(contentUri, null);

            }

        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的uri，则使用普通方式使用
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的uri，直接获取路径即可
            imagePath = uri.getPath();

        }

        displayImage(imagePath);
    }

    /**
     * 检测人脸图片，剪裁人脸并处理成向量，以字符串形式发送到服务器。
     * @param faceBitmap 人脸Bitmap
     */

    void processFaceBitmap(Bitmap faceBitmap){
        StringBuilder faceStrBuilder = new StringBuilder();
        List<FaceAttr> facesList =  mtcnn.detect(faceBitmap);
        try {
            float maxProb = 0f;
            Rect rect = new Rect();
            //  找到最大可能性的脸
            for (FaceAttr face : facesList) {
                float prob = face.getProb();
                if (prob > maxProb) {
                    maxProb = prob;
                    RectF rectF = face.getBbox();
                    rectF.round(rect);
                }
            }
            if (maxProb >= 0.98){
                Bitmap resizedBitmap = resizeImage(faceBitmap,rect.left,rect.top,rect.width(),
                        rect.height(),FACE_SIZE,FACE_SIZE);
                faceImageView.setImageBitmap(resizedBitmap);
                //临时将名字加入NameFacesDBMap,便于在显示列表及时更新
                NameFacesDBMap.put(personName, " ");
                result_textView.setText("上一次添加的人脸如上，姓名为："+personName);

                float[] embeddings = faceRecognizer.getFaceEmbeddings(resizedBitmap);
                //  保存的信息形式为 姓名:0.1 0.1 0.1 ……
                Log.d("Len of FaceEmbeddings:", ""+embeddings.length);

                faceStrBuilder.append(personName).append(":");
                for(float embedding:embeddings){
                    faceStrBuilder.append(embedding).append(" ");
                }

                faceStrBuilder.deleteCharAt(faceStrBuilder.length()-1);   // 去掉最后的空格
                String new_face_str = faceStrBuilder.toString();

                OkHttpClient okHttpClient = new OkHttpClient();
                FormBody.Builder formBody = new FormBody.Builder();
                formBody.add("new_face", new_face_str);
                Request request = new Request.Builder()//创建Request 对象。
                        .url(UPDATE_URL)
                        .post(formBody.build())//传递请求体
                        .build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        speech.say(getString(R.string.fail_update_face));
                        Log.d("OKHTTP", "onFailure: ");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d("OKHTTP", "onResponse: ");
                        speech.say(getString(R.string.succeed_update_face));
                    }
                });
            }
            else{
                Toast.makeText(MainActivity.this,
                        getString(R.string.face_not_found), Toast.LENGTH_LONG);
                speech.say(getString(R.string.face_not_found));
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        //then send to server
    }


//
//    /**
//     * 文件下载
//     */
//    private void downloadFile(String url) {
//        DownloadUtil.get().download(url, DestFileDir, DestFileName,
//                new DownloadUtil.OnDownloadListener() {
//                    @Override
//                    public void onDownloadSuccess(File file) {
//                        Log.d("face_data", "onDownloadSuccess: ");
//                        speech.say("文件下载成功！");
//
//                    }
//
//                    @Override
//                    public void onDownloading(int progress) {
//                    }
//
//                    @Override
//                    public void onDownloadFailed(Exception e) {
//                        Log.e("face_data", "onDownloadFailed: ", e);
//                        speech.say("文件下载失败!");
//                    }
//                });
//    }

    /**
     * 检查目标Ip是否可以访问
     * @param ip 被检查是否可以访问的IP
     * @return True or False
     */
//    public boolean isIpAvailable(String ip) {
//
//        Runtime runtime = Runtime.getRuntime();
//        Process ipProcess = null;
//        try {
//            ipProcess = runtime.exec("ping -c 1 -w 2 "+ip);
//            InputStream input = ipProcess.getInputStream();
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(input));
//            StringBuffer stringBuffer = new StringBuffer();
////            String content = "";
//            String content = in.readLine();
//            while (content != null) {
//                stringBuffer.append(content);
//            }
//
//            int exitValue = ipProcess.waitFor();
//            Log.d("exitValue", "isIpAvailable: "+exitValue);
//            Log.d("content", "isIpAvailable: "+content);
//            if (exitValue == 0) {
//                //WiFi连接，网络正常
//                return true;
//            } else {
//                if (stringBuffer.indexOf("100% packet loss") != -1) {
//                    //网络丢包严重，判断为网络未连接
//                    return false;
//                } else {
//                    //网络未丢包，判断为网络连接
//                    return true;
//                }
//            }
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            if (ipProcess != null) {
//                ipProcess.destroy();
//            }
//            runtime.gc();
//        }
//        return false;
//    }

}
