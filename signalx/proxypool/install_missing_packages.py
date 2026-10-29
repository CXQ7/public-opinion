import importlib
import subprocess
import sys


def install_missing_packages():
    """装饰器函数：用于自动安装缺失的Python包"""

    def decorator(func):
        def wrapper(*args, **kwargs):
            while True:
                try:
                    return func(*args, **kwargs)
                except ModuleNotFoundError as e:
                    # 提取缺失的包名
                    missing_module = str(e).split("'")[1]
                    print(f"发现缺失的包：{missing_module}，正在尝试安装...")

                    # 使用pip安装缺失的包
                    try:
                        subprocess.check_call([sys.executable, "-m", "pip", "install", missing_module])
                        print(f"{missing_module} 安装成功！")

                        # 重新加载模块（如果可能）
                        try:
                            importlib.invalidate_caches()
                            globals()[missing_module] = importlib.import_module(missing_module)
                        except:
                            pass
                    except subprocess.CalledProcessError as pe:
                        print(f"安装 {missing_module} 失败：{str(pe)}")
                        sys.exit(1)

        return wrapper

    return decorator


# 使用示例
@install_missing_packages()
def main():
    # 这里编写你的主程序
    import requests  # 如果没有安装，会自动尝试安装
    response = requests.get('https://www.example.com')
    print(f"请求状态码：{response.status_code}")


if __name__ == "__main__":
    main()
