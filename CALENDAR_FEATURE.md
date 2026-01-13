# Calendar Feature Implementation Guide

## Tổng quan
Đã implement thành công tính năng Calendar View cho Todo App với đầy đủ chức năng:
- Hiển thị calendar với dots indicator cho các ngày có tasks
- Filter tasks theo ngày được chọn
- Hiển thị danh sách tasks trong RecyclerView
- Toggle done, delete, và mở Pomodoro timer

## Files đã tạo/sửa

### 1. Dependencies (build.gradle.kts)
✅ Thêm MaterialCalendarView library
✅ Thêm JitPack repository trong settings.gradle.kts

### 2. Database Layer

#### TodoDao.kt
```kotlin
@Query("SELECT * FROM todos WHERE dueAt >= :startOfDay AND dueAt < :endOfDay ORDER BY dueAt ASC")
suspend fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): List<TodoEntity>

@Query("SELECT DISTINCT date(dueAt / 1000, 'unixepoch', 'localtime') as date FROM todos WHERE dueAt IS NOT NULL")
suspend fun getAllDatesWithTasks(): List<String>
```

#### TodoRepository.kt
```kotlin
suspend fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): List<TodoEntity>
suspend fun getAllDatesWithTasks(): List<String>
```

### 3. ViewModel Layer

#### CalendarViewModel.kt (NEW)
- `CalendarState`: data class chứa selectedDate, tasksForSelectedDate, datesWithTasks
- `selectDate(calendar)`: Load tasks cho ngày được chọn
- `toggleDone(todo)`: Toggle trạng thái done
- `deleteTodo(todo)`: Xóa task

### 4. UI Layer

#### activity_calendar.xml (NEW)
- MaterialCalendarView để hiển thị calendar
- TextView hiển thị ngày được chọn
- RecyclerView hiển thị list tasks
- TextView "No tasks" khi không có tasks

#### CalendarActivity.kt (NEW)
- Setup ViewBinding
- Setup RecyclerView với TodoAdapter
- Implement calendar date selection
- TaskDotDecorator: custom decorator để hiển thị dots trên các ngày có tasks
- Observe CalendarViewModel state và update UI

### 5. Resources

#### strings.xml
```xml
<string name="calendar_title">Task Calendar</string>
<string name="select_date_prompt">Select a date to view tasks</string>
<string name="no_tasks_for_date">No tasks for this date</string>
<string name="btn_open_calendar">Calendar</string>
```

#### Drawables
- `ic_back.xml`: Back arrow icon cho toolbar
- `ic_calendar.xml`: Calendar icon cho button

### 6. MainActivity.kt
✅ Thêm button "Calendar" vào layout
✅ Thêm click listener để mở CalendarActivity

### 7. AndroidManifest.xml
✅ Đăng ký CalendarActivity

## Cách build và test

1. **Sync Project với Gradle**
   ```
   File > Sync Project with Gradle Files
   ```

2. **Build Project**
   ```
   Build > Make Project
   hoặc
   ./gradlew assembleDebug
   ```

3. **Chạy app trên emulator/device**
   - Mở MainActivity
   - Click button "Calendar" 
   - Calendar sẽ hiển thị dots màu đỏ trên các ngày có tasks
   - Click vào một ngày để xem tasks
   - Có thể toggle done/delete tasks trực tiếp

## Kiến trúc Flow

```
[CalendarActivity] 
    ↓ (ViewBinding)
[CalendarViewModel]
    ↓ (StateFlow<CalendarState>)
[TodoRepository]
    ↓
[TodoDao] → Room Database
```

## Features

### 1. Visual Indicators
- Dots màu đỏ (#FF6B6B) hiển thị trên các ngày có tasks
- Sử dụng DotSpan từ MaterialCalendarView

### 2. Date Selection
- Default: select ngày hiện tại
- Click vào ngày → load tasks của ngày đó
- Hiển thị format: "EEEE, dd MMMM yyyy" (ví dụ: "Monday, 12 January 2026")

### 3. Task List
- Hiển thị trong RecyclerView với TodoAdapter hiện có
- Hỗ trợ toggle done
- Hỗ trợ delete
- Hỗ trợ mở Pomodoro timer
- Edit task → navigate về MainActivity (placeholder)

### 4. Empty State
- Hiển thị "No tasks for this date" khi không có tasks

## Lưu ý khi develop

1. **MaterialCalendarView dependency**: Đảm bảo JitPack repository được thêm vào settings.gradle.kts

2. **Date Format**: 
   - Database lưu `dueAt` dạng Long (epoch millis)
   - Query sử dụng start/end of day để lấy chính xác tasks trong ngày
   - SQL query dùng `date()` function để extract date string

3. **Calendar Month Index**: MaterialCalendarView dùng month 1-12, nhưng Java Calendar dùng 0-11. Đã handle conversion trong code.

4. **Decorator Performance**: Chỉ re-create decorators khi datesWithTasks thay đổi để tối ưu performance.

## Potential Enhancements

1. **Different colors** cho priority levels
2. **Long press** để add task trực tiếp từ calendar
3. **Month view statistics** (số lượng tasks per month)
4. **Week view** option
5. **Integrate với Eisenhower Matrix** (filter by tag)

## Testing Checklist

- [ ] Calendar hiển thị đúng tháng hiện tại
- [ ] Dots hiển thị trên các ngày có tasks
- [ ] Click vào ngày → tasks được load
- [ ] Toggle done → update database và UI
- [ ] Delete task → xóa khỏi database và UI
- [ ] Navigate back → về MainActivity
- [ ] Empty state hiển thị đúng
- [ ] Rotation device → state được preserve
