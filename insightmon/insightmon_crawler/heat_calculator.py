import constant
import pandas as pd


def cal_heat(data, debug=False, output_as_file=False):
    data_with_heat = pd.DataFrame()
    for source, params_dict in constant.HEAT_PARAMS_DICT.items():
        domain_data = data[data["source"] == source]
        # 将点赞、评论和转发转为read
        domain_data.loc[:, 'read_count'] += (5 * domain_data.loc[:, 'like_count']
                                             + 10 * domain_data.loc[:, 'comment_count']
                                             + 20 * domain_data.loc[:, 'share_count'])
        total_read = sum(domain_data.loc[:, 'read_count'])
        if len(domain_data) != 0:
            average_read = total_read / len(domain_data)
        else:
            average_read = 0

        if average_read == 0:
            domain_data['heat'] = params_dict['BIAS']
        else:
            domain_data.loc[:, 'heat'] = (params_dict['BIAS']
                                                 + params_dict['WEIGHT']
                                                 * domain_data.loc[:, 'read_count'] / average_read)

        domain_data = domain_data.drop(['read_count', 'like_count', 'comment_count', 'share_count'], axis=1)
        data_with_heat = pd.concat([data_with_heat, domain_data], axis=0, ignore_index=True)
        if debug:
            print("source: ", source)
            print("total_read, avg_read: ", total_read, average_read)
            print("domain_data: ", domain_data)
    if debug:
        print("data_with_heat: ", data_with_heat)
    if output_as_file:
        data_with_heat.to_csv('./data/data_with_heat.csv', encoding="utf_8_sig", index_label='event_id')
    return data_with_heat


# TODO: this is just a demo
if __name__ == "__main__":
    data = pd.read_csv("./data/all_info.csv", index_col='event_id')
    cal_heat(data, True, True)
