package com.aidl.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.aidl.IStudentAddListener;
import com.aidl.Student;
import com.aidl.StudentManager;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ScrollView scrollView;
    private TextView tvLog;
    private Button btAdd;
    private Button btGet;

    private StringBuffer log = new StringBuffer();

    //由AIDL文件生成的Java类
    private StudentManager manager = null;

    //标志当前与服务端连接状况的布尔值，false为未连接，true为连接中
    private boolean mBound = false;

    private static final int NEW_STUDENT = 101;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEW_STUDENT:
                    setLog("服务端通知增加了一名学生");
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        tvLog = (TextView) findViewById(R.id.log);
        btAdd = (Button) findViewById(R.id.add);
        btGet = (Button) findViewById(R.id.get);

        btAdd.setOnClickListener(this);
        btGet.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBound) {
            attemptToBindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            if (manager != null) {
                try {
                    manager.unregisterListener(studentAddListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    /**
     * 检查连接状态
     *
     * @return
     */
    private boolean checkConnect() {
        if (!mBound) {
            attemptToBindService();
            Toast.makeText(this, "正在尝试重连，请稍后再试", Toast.LENGTH_SHORT).show();
            setLog("正在尝试重连，请稍后再试");
            return true;
        }
        if (manager != null) return true;
        return false;
    }

    /**
     * 定义一个死亡代理对象
     */
    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (manager == null) return;
            manager.asBinder().unlinkToDeath(deathRecipient, 0);
            manager = null;
            mBound = false;
            // 重连
            attemptToBindService();
        }
    };

    /**
     * 增加学生的监听
     */
    private IStudentAddListener studentAddListener = new IStudentAddListener.Stub() {

        @Override
        public void onStudentAdd(Student s) throws RemoteException {
            mHandler.sendEmptyMessage(NEW_STUDENT);
        }
    };

    /**
     * 尝试与服务端建立连接
     */
    private void attemptToBindService() {
        Intent intent = new Intent();
        intent.setAction("STUDENT_SERVICE");
        intent.setPackage("com.aidl");
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                setLog("service connected");
                manager = StudentManager.Stub.asInterface(service);
                mBound = true;
                // 设置死亡代理
                service.linkToDeath(deathRecipient, 0);
                // 注册增加学生的监听
                manager.registerListener(studentAddListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            setLog("service disconnected");
            mBound = false;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add:
                addStudent();
                break;
            case R.id.get:
                getStudents();
                break;
            default:
                break;
        }
    }

    Random random = new Random();

    private void addStudent() {
        if (!checkConnect()) return;
        try {
            Student s = new Student("s" + random.nextInt(1000), random.nextInt(20));
            manager.addStudent(s);
            setLog("add student success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getStudents() {
        if (!checkConnect()) return;
        try {
            List<Student> list = manager.getStudents();
            setLog("get students success list.size()=" + list.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLog(String msg) {
        log.append(msg + "\n");
        tvLog.setText(log);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }
}
