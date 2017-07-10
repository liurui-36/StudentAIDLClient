// StudentManager.aidl
package com.aidl;

import com.aidl.Student;
import com.aidl.IStudentAddListener;

interface StudentManager {
    //所有的返回值前都不需要加任何东西，不管是什么数据类型
    List<Student> getStudents();

    //传参时除了Java基本类型以及String，CharSequence之外的类型
    //都需要在前面加上定向tag，具体加什么量需而定
    void addStudent(in Student s);

    // 注册增加学生的监听
    void registerListener(IStudentAddListener listener);
    // 解除注册增加学生的监听
    void unregisterListener(IStudentAddListener listener);
}
