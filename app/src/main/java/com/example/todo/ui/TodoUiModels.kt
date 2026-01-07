package com.example.todo.ui

enum class Filter { ALL, ACTIVE, DONE }

enum class Sort {
    CREATED_DESC, // Mới nhất
    CREATED_ASC,  // Cũ nhất
    DUE_ASC,      // Gần đến hạn
    DUE_DESC,     // Xa đến hạn
    PRIORITY_DESC,// Ưu tiên cao trước
    PRIORITY_ASC  // Ưu tiên thấp trước
}

enum class TodoGroup { ALL, UPCOMING, DO_NOW, SCHEDULE, DELEGATE, ELIMINATE }
