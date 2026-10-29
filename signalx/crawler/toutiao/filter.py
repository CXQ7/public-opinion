import pandas as pd


def Filter(data_before):
    if not isinstance(data_before, list):
        raise ValueError("传入的数据必须是列表类型")
    for item in data_before:
        if not isinstance(item, dict):
            raise ValueError("列表中的元素必须是字典类型")
    df = pd.DataFrame(data_before)
    filtered_date = df[df['url'].notnull()]
    pd.set_option('display.max_columns', None)  # 显示所有列
    pd.set_option('display.max_rows', None)  # 显示所有行
    pd.set_option('display.width', None)  # 自动调整列宽
    pd.set_option('display.max_colwidth', 100)  # 设置列的最大宽度
    if isinstance(filtered_date, pd.DataFrame):
        filtered_date = filtered_date.to_dict(orient='records')  # 将 DataFrame 转换为列表类型的字典
    return filtered_date


if __name__ == "__main__":
    data = Filter()
    print(data)
