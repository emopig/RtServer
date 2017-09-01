---------------------------------------------------
-- Export file for user LJ                       --
-- Created by Zengwang Lin on 2017/9/1, 15:11:08 --
---------------------------------------------------

spool oradb.log

prompt
prompt Creating table LJ_RESULT
prompt ========================
prompt
create global temporary table LJ_RESULT
(
  row_id   VARCHAR2(30),
  keyword  VARCHAR2(100),
  priority NUMBER(3)
)
on commit preserve rows;

prompt
prompt Creating view LJ_V_CONTENT
prompt ==========================
prompt
create or replace view lj_v_content as
Select Rowid row_id,lj_no || '/' || item_brand || '/'
    || item_no || '/' || item_name|| '/' ||  car_style|| '/' ||  dir_no|| '/'
    ||  brand_color|| '/' ||  brand_type|| '/' ||  item_group_cust2|| '/'
    ||  item_group_cust3 || '/' || item_group_cust4 Content
     From lj_item;

prompt
prompt Creating procedure LJ_P_SEARCH
prompt ==============================
prompt
create or replace procedure lj_p_search(p_keywords In Varchar2,p_row_from In Number,p_row_to In Number,p_rows Out sys_refcursor,p_err Out Varchar2) Is
  v_priority Number(3) := 1;

  
  Procedure p_get_word(pp_keywords Varchar2) Is
    v_keywords Varchar2(1000);
    v_word     Varchar2(100);
  Begin
    Select Trim(pp_keywords) Into v_keywords
      From Dual;
    Select Replace(v_keywords, Chr(9), ' ') Into v_keywords
      From Dual;
  
    If instr(v_keywords, ' ') > 0 Then
      v_word := Substr(pp_keywords, 1, instr(v_keywords, ' ')-1);
      --
      Insert Into lj_result(row_id, keyword, priority)
        Select row_id, v_word, v_priority
          From lj_v_content
         Where content Like '%' || v_word || '%';
    
      v_priority := v_priority + 1;
      p_get_word(Replace(v_keywords, v_word));
    Else
      v_word := v_keywords;
      Insert Into lj_result(row_id, keyword, priority)
        Select row_id, v_word, v_priority
          From lj_v_content
         Where content Like '%' || v_word || '%';
    End If;
  End;  

Begin
  Delete lj_result;
  p_get_word(p_keywords);
  
  Commit;
  
  Open p_rows For 
    Select a.row_num, a.row_id,b.Content
      From (Select Rownum row_num, row_id
              From (Select row_id, priority,Count(Distinct keyword) tms From LJ_RESULT Group By row_id,priority Order By tms Desc,priority)
             Where Rownum < 100) a,lj_v_content b          
     Where a.row_id = b.row_id
       And a.row_num Between p_row_from And p_row_to;
       
Exception
  When Others Then
    p_err := 'ÒâÍâ´íÎó:' || Substr(Sqlerrm,1,300);
End;
/


spool off
