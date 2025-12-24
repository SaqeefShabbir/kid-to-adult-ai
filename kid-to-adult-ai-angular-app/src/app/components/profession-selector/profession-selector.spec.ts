import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfessionSelector } from './profession-selector';

describe('ProfessionSelector', () => {
  let component: ProfessionSelector;
  let fixture: ComponentFixture<ProfessionSelector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfessionSelector]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfessionSelector);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
