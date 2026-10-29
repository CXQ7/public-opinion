from datetime import datetime, timedelta
import time

def id_to_time(id_str):
    utc8_seconds = int(id_str[:8], 16)  # 前8位转十进制
    utc_time = datetime.utcfromtimestamp(utc8_seconds) + timedelta(hours=8)
    return utc_time

if __name__ == '__main__':
    # now_utc8 = datetime.now(timezone(timedelta(hours=8)))
    # print(str(int(datetime.now().timestamp() * 1000) - 28800 * 1000))
    print(str(int(datetime.now().timestamp() * 1000)))
    print(str(int(time.time() * 1000)))
    # print(str(int(now_utc8.timestamp() * 1000)))
    # 1748620914529

    print(id_to_time("677978ac000000000800c7d3").strftime("%Y-%m-%d %H:%M:%S UTC+8"))  # 输出：2025-01-04 09:00:00 UTC+8
    print(id_to_time("67349bac000000001b02e8eb").strftime("%Y-%m-%d %H:%M:%S UTC+8"))
    print(id_to_time("5e9e8c880000000001003373").strftime("%Y-%m-%d %H:%M:%S UTC+8"))
    print(id_to_time("6756f9c9000000000700b2d9").strftime("%Y-%m-%d %H:%M:%S UTC+8"))
    print(id_to_time("6837d3ba0000000021005e2a").strftime("%Y-%m-%d %H:%M:%S UTC+8"))

    print(id_to_time("68391fef0000000021004b0d").strftime("%Y-%m-%d %H:%M:%S UTC+8"))  # 昨天11:03
