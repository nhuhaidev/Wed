import os
import time
import pandas as pd
import pyodbc
import json
import sys
import io

# Ép hệ thống xuất log ra Terminal bằng định dạng UTF-8 để hỗ trợ Tiếng Việt
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8',line_buffering=True)
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8',line_buffering=True)
def get_db_connection():
    conn_str = (
        r'DRIVER={ODBC Driver 17 for SQL Server};'
        r'SERVER=localhost;'
        r'DATABASE=AntiCheatDB;'
        r'Trusted_Connection=yes;'
    )
    return pyodbc.connect(conn_str)

def safe_batch_etl(raw_file_path):
    # Đường dẫn file tạm để xử lý
    processing_file_path = raw_file_path.replace("raw_server_events.json", "processing_events.json")

    # ==========================================
    # BƯỚC 1: CẮT FILE (Atomic Rename)
    # ==========================================
    if not os.path.exists(raw_file_path):
        print("Trạng thái: Không có dữ liệu log mới để xử lý.")
        return

    try:
        # Đổi tên file để "cách ly" dữ liệu khỏi Java Plugin
        os.rename(raw_file_path, processing_file_path)
        print(f"🔄 [ROTATE] Đã khóa file thô thành: {processing_file_path}")
    except PermissionError:
        print("⚠️ [CẢNH BÁO] File đang bị Java giữ quyền (Lock). Sẽ thử lại ở chu kỳ sau.")
        return

    # ==========================================
    # BƯỚC 2: EXTRACT & TRANSFORM (Đọc dữ liệu)
    # ==========================================
    print("🚀 [EXTRACT] Bắt đầu đọc dữ liệu từ file processing...")
    data = []
    with open(processing_file_path, 'r', encoding='utf-8') as file:
        for line in file:
            try:
                data.append(json.loads(line.strip()))
            except json.JSONDecodeError:
                continue

    df = pd.DataFrame(data)
    if df.empty:
        print("⚠️ File log rỗng. Dọn dẹp và thoát.")
        os.remove(processing_file_path)
        return

    print(f"✅ Đã tải thành công {len(df)} sự kiện. Bắt đầu đẩy vào SQL Server...")

    # ==========================================
    # BƯỚC 3: LOAD (Nạp vào Database)
    # ==========================================
    conn = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 1. Dữ liệu Di chuyển
        df_move = df[df['event_type'] == 'move'].dropna(subset=['x', 'y', 'z'])
        if not df_move.empty:
            insert_move = "INSERT INTO Log_Movement (uuid, timestamp, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)"
            for _, row in df_move.iterrows():
                cursor.execute(insert_move, row['uuid'], row['timestamp'], row['x'], row['y'], row['z'], row['yaw'], row['pitch'])
            conn.commit()

        # 2. Dữ liệu Chiến đấu
        df_combat = df[df['event_type'] == 'combat']
        if not df_combat.empty:
            insert_combat = "INSERT INTO Log_Combat (uuid, timestamp, target_type, distance, damage_dealt) VALUES (?, ?, ?, ?, ?)"
            for _, row in df_combat.iterrows():
                cursor.execute(insert_combat, row['uuid'], row['timestamp'], row['target_type'], row['distance'], row['damage_dealt'])
            conn.commit()

        # 3. Dữ liệu Đào khối
        df_mine = df[df['event_type'] == 'mine']
        if not df_mine.empty:
            insert_mine = "INSERT INTO Log_Mining (uuid, timestamp, block_type, block_x, block_y, block_z, light_level) VALUES (?, ?, ?, ?, ?, ?, ?)"
            for _, row in df_mine.iterrows():
                cursor.execute(insert_mine, row['uuid'], row['timestamp'], row['block_type'], row['block_x'], row['block_y'], row['block_z'], row['light_level'])
            conn.commit()

        # 4. Dữ liệu Túi đồ
        df_inv = df[df['event_type'] == 'inventory']
        if not df_inv.empty:
            insert_inv = "INSERT INTO Log_Inventory (uuid, timestamp, action_type, slot_clicked, is_shift_click) VALUES (?, ?, ?, ?, ?)"
            for _, row in df_inv.iterrows():
                is_shift = 1 if row['is_shift_click'] else 0
                cursor.execute(insert_inv, row['uuid'], row['timestamp'], row['action_type'], row['slot_clicked'], is_shift)
            conn.commit()

        print("🎉 [SUCCESS] Đã Load toàn bộ dữ liệu vào Database!")

        # ==========================================
        # BƯỚC 4: DỌN DẸP (Xóa file đã xử lý xong)
        # ==========================================
        cursor.close()
        conn.close()
        os.remove(processing_file_path)
        print("🗑️ [CLEANUP] Đã xóa file tạm. Chu kỳ ETL hoàn tất an toàn.")

    except Exception as e:
        print(f"❌ Lỗi nghiêm trọng trong quá trình nạp DB: {e}")
        print("⚠️ Giữ lại file processing_events.json để debug.")
        if conn:
            conn.rollback()

if __name__ == "__main__":
    # Trỏ đúng đường dẫn tuyệt đối tới file của server
    # Ví dụ: r"D:\DoAnNganh\PaperServer\plugins\DataCollector\raw_server_events.json"
    
    # SỬA LẠI ĐƯỜNG DẪN NÀY CHO ĐÚNG VỚI TÊN FILE MỚI
    file_path = r"D:\DoAnNganh\PaperServer\plugins\DataCollector\raw_server_events.json" 
    
    print("🤖 Hệ thống ETL Pipeline đã khởi động. Đang lắng nghe dữ liệu...")
    while True:
        try:
            safe_batch_etl(file_path)
        except Exception as e:
            print(f"Lỗi không xác định: {e}")
        time.sleep(30)