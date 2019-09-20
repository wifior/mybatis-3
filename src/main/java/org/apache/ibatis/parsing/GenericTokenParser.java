/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  /**
   * 开始的Token字符串
   */
  private final String openToken;

  /**
   * 结束的Token字符串
   */
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    //寻找开始的openToken 的位置
    int start = text.indexOf(openToken);
    if (start == -1) {//找不到，直接返回
      return text;
    }
    char[] src = text.toCharArray();
    int offset = 0;//起始查找位置

    //结果
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null; //匹配到openToken和closeToken之间的表达式

    //循环匹配
    while (start > -1) {
      //转义字符
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        //因为openToken前面一个位置是 \ 转义字符，所以忽略 \
        //添加 [offset,start - offset -1]和openToken的内容，添加到builder中
        builder.append(src, offset, start - offset - 1).append(openToken);
        //修改offset
        offset = start + openToken.length();
        //非转义字符
      } else {
        // found open token. let's search close token.
        //创建/重置expression对象
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        //添加offset和openToken之间的内容，添加到builder中
        builder.append(src, offset, start - offset);
        //修改offset
        offset = start + openToken.length();
        //寻找结束的closeToken的位置
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          //转义
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            //因为endToken前面一个位置是\转义字符，所以忽略\
            //添加[offset,end-offset-1]和endToken的内容，添加到builder中
            expression.append(src, offset, end - offset - 1).append(closeToken);
            //修改offset
            offset = end + closeToken.length();
            //继续，寻找结束的closeToken的位置
            end = text.indexOf(closeToken, offset);
            //非转义
          } else {
            //添加[offset,end-offset的内容，添加到builder中]
            expression.append(src, offset, end - offset);
            break;
          }
        }
        //拼接内容
        if (end == -1) {
          // close token was not found.
          //closeToken未找到，直接拼接
          builder.append(src, start, src.length - start);
          //修改offset
          offset = src.length;
        } else {
          //<x> closeToken找到，将expression提交给handler处理，并将处理结果添加 到handler中
          builder.append(handler.handleToken(expression.toString()));
          //修改offset
          offset = end + closeToken.length();
        }
      }
      //继续，寻找结束的openToken的位置
      start = text.indexOf(openToken, offset);
    }
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
