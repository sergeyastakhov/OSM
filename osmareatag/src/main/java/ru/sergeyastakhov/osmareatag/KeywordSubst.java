/**
 * KeywordSubst.java
 * <p>
 * Copyright (C) 2017 RNIC. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class KeywordSubst
{
  /**
   * Интерфейс словаря для подстановок
   */
  public interface Dictionary
  {
    Object get(String keyword);
  }

  /**
   * Словарь-обертка для обычного Map-а
   */
  private static class DictMapAdapter implements Dictionary
  {
    private final Map<String, ?> map;

    DictMapAdapter(Map<String, ?> _map) { map = _map; }

    public Object get(String keyword) { return map.get(keyword); }
  }

  private Dictionary keywords;

  private String start;
  private String end;

  private int startLen;
  private int endLen;

  /**
   * @param _keywords набор строк ключ=значение для выполнения подстановок
   */
  public KeywordSubst(Map<String, ?> _keywords)
  {
    this(new DictMapAdapter(_keywords));
  }

  /**
   * @param _keywords набор строк ключ=значение для выполнения подстановок
   * @param _start    признак начала подстановки
   * @param _end      признак конца подстановки
   */
  public KeywordSubst(Map<String, ?> _keywords, String _start, String _end)
  {
    this(new DictMapAdapter(_keywords), _start, _end);
  }

  public KeywordSubst(Dictionary _keywords)
  {
    this(_keywords, "${", "}");
  }

  /**
   * @param _keywords набор строк ключ=значение для выполнения подстановок
   * @param _start    признак начала подстановки
   * @param _end      признак конца подстановки
   */
  public KeywordSubst(Dictionary _keywords, String _start, String _end)
  {
    keywords = _keywords;
    start = _start;
    end = _end;

    startLen = start.length();
    endLen = end.length();
  }

  /**
   * Выполняет замену подстановок с использованием словаря, переданного при создании
   *
   * @param str строка для подстановок
   * @return результат подстановки значений
   */
  public String resolve(String str)
  {
    int index = str.indexOf(start, 0);
    if( index < 0 ) return str;

    StringBuilder result = new StringBuilder(str.length());

    int prevIndex = 0;

    while( index >= 0 )
    {
      if( prevIndex < index )
        result.append(str, prevIndex, index);

      int keywordStart = index + startLen;

      int keywordEnd = str.indexOf(end, keywordStart);

      if( keywordEnd < 0 )
      {
        // Если окончания для keyword не найдено, оставляем остаток строки неизменным
        prevIndex = index;
        break;
      }

      String keyword = str.substring(keywordStart, keywordEnd);

      Object value = keywords.get(keyword);

      if( value != null )
      {
        result.append(value);
      }
      else
      {
        // Если ключевое слово неизвестно то оставляем его без изменений
        result.append(str, index, keywordEnd + endLen);
      }

      prevIndex = keywordEnd + endLen;
      index = str.indexOf(start, prevIndex);
    }

    // Добавление остатка строки без ключевых слов
    if( prevIndex < str.length() )
    {
      result.append(str, prevIndex, str.length());
    }

    return result.toString();
  }
}
